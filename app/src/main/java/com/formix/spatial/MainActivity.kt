package com.formix.spatial

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.formix.spatial.camera.OrbitCamera
import com.formix.spatial.databinding.ActivityMainBinding
import com.formix.spatial.gl.SceneRenderer
import com.formix.spatial.history.AddObjectCommand
import com.formix.spatial.history.DeleteObjectCommand
import com.formix.spatial.history.UndoManager
import com.formix.spatial.input.GestureController
import com.formix.spatial.input.ToolMode
import com.formix.spatial.io.ProjectSerializer
import com.formix.spatial.scene.PrimitiveType
import com.formix.spatial.scene.Scene
import com.formix.spatial.scene.SceneObject
import com.formix.spatial.scene.Transform

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: SceneRenderer
    private lateinit var gestureController: GestureController

    private val scene = Scene()
    private val camera = OrbitCamera()
    private val undoManager = UndoManager()

    private val toolButtons by lazy {
        mapOf(
            ToolMode.NAVIGATE to binding.btnToolNavigate,
            ToolMode.MOVE to binding.btnToolMove,
            ToolMode.ROTATE to binding.btnToolRotate,
            ToolMode.SCALE to binding.btnToolScale
        )
    }

    private val palette = listOf(
        floatArrayOf(0.55f, 0.62f, 0.95f, 1f),
        floatArrayOf(0.95f, 0.55f, 0.45f, 1f),
        floatArrayOf(0.45f, 0.85f, 0.65f, 1f),
        floatArrayOf(0.95f, 0.78f, 0.35f, 1f),
        floatArrayOf(0.75f, 0.55f, 0.95f, 1f)
    )
    private var nextColorIdx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewport()
        setupGestures()
        setupToolbar()
        setupUndoRedoUi()
        setSelectedTool(ToolMode.NAVIGATE)

        if (savedInstanceState == null) {
            // Seed an empty scene; user starts fully blank per spec (Core Concept #1).
        }
    }

    private fun setupViewport() {
        renderer = SceneRenderer(scene, camera)
        renderer.onFrameStats = { fps, objectCount, vertexCount ->
            runOnUiThread {
                binding.statsLabel.text = "$fps FPS\nObjects: $objectCount\nVerts: $vertexCount"
            }
        }

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        binding.viewportContainer.addView(glView)
    }

    private fun setupGestures() {
        gestureController = GestureController(scene, camera, renderer)
        gestureController.onSelectionChanged = { /* reserved for future inspector panel */ }
        gestureController.onTransformCommitted = { cmd -> undoManager.execute(cmd) }
        glView.setOnTouchListener(gestureController)
    }

    private fun setupToolbar() {
        binding.btnAddCube.setOnClickListener { addPrimitive(PrimitiveType.CUBE) }
        binding.btnAddSphere.setOnClickListener { addPrimitive(PrimitiveType.SPHERE) }
        binding.btnAddCylinder.setOnClickListener { addPrimitive(PrimitiveType.CYLINDER) }

        binding.btnToolNavigate.setOnClickListener { setSelectedTool(ToolMode.NAVIGATE) }
        binding.btnToolMove.setOnClickListener { setSelectedTool(ToolMode.MOVE) }
        binding.btnToolRotate.setOnClickListener { setSelectedTool(ToolMode.ROTATE) }
        binding.btnToolScale.setOnClickListener { setSelectedTool(ToolMode.SCALE) }
        binding.btnToolDelete.setOnClickListener { deleteSelected() }

        binding.btnSave.setOnClickListener { saveProject() }
        binding.btnLoad.setOnClickListener { loadProject() }
    }

    private fun setupUndoRedoUi() {
        binding.btnUndo.setOnClickListener { undoManager.undo() }
        binding.btnRedo.setOnClickListener { undoManager.redo() }
        undoManager.onChanged = { refreshUndoRedoButtons() }
        refreshUndoRedoButtons()
    }

    private fun refreshUndoRedoButtons() {
        binding.btnUndo.isEnabled = undoManager.canUndo()
        binding.btnRedo.isEnabled = undoManager.canRedo()
        binding.btnUndo.alpha = if (undoManager.canUndo()) 1f else 0.4f
        binding.btnRedo.alpha = if (undoManager.canRedo()) 1f else 0.4f
    }

    private fun setSelectedTool(mode: ToolMode) {
        gestureController.toolMode = mode
        for ((m, button) in toolButtons) {
            button.isSelected = (m == mode)
            paintToolButton(button, m == mode)
        }
    }

    private fun paintToolButton(button: Button, active: Boolean) {
        button.setBackgroundColor(
            resources.getColor(
                if (active) R.color.sf_tool_active else R.color.sf_tool_inactive, theme
            )
        )
        button.setTextColor(
            resources.getColor(
                if (active) android.R.color.black else R.color.sf_text_primary, theme
            )
        )
    }

    private fun addPrimitive(type: PrimitiveType) {
        val id = scene.newId()
        val transform = Transform(px = 0f, py = 0.5f, pz = 0f)
        val color = palette[nextColorIdx % palette.size]
        nextColorIdx++
        val obj = SceneObject(id, type, transform, color)
        undoManager.execute(AddObjectCommand(scene, obj))
    }

    private fun deleteSelected() {
        val obj = scene.selected()
        if (obj == null) {
            Toast.makeText(this, R.string.toast_select_first, Toast.LENGTH_SHORT).show()
            return
        }
        undoManager.execute(DeleteObjectCommand(scene, obj))
    }

    private fun saveProject() {
        val ok = ProjectSerializer.save(this, scene)
        Toast.makeText(this, if (ok) R.string.toast_saved else R.string.toast_no_save, Toast.LENGTH_SHORT).show()
    }

    private fun loadProject() {
        if (!ProjectSerializer.hasSavedProject(this)) {
            Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show()
            return
        }
        val ok = ProjectSerializer.load(this, scene)
        Toast.makeText(this, if (ok) R.string.toast_loaded else R.string.toast_no_save, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Autosave on backgrounding so a crash/kill doesn't lose work (Section 25).
        ProjectSerializer.save(this, scene)
        glView.onPause()
    }
}
