package com.formix.spatial.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.formix.spatial.camera.OrbitCamera
import com.formix.spatial.scene.PrimitiveType
import com.formix.spatial.scene.Scene
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SceneRenderer(
    private val scene: Scene,
    private val camera: OrbitCamera
) : GLSurfaceView.Renderer {

    private var program = 0
    private var uMVP = 0
    private var uModel = 0
    private var uNormalMatrix = 0
    private var uColor = 0
    private var uLightDir = 0
    private var uCameraPos = 0
    private var uSelected = 0

    private val meshes = HashMap<PrimitiveType, Mesh>()

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val normalMatrix3 = FloatArray(9)
    private val normalMatrix4 = FloatArray(16)

    // Exposed for picking / gizmo math (read on GL thread copies via getters below).
    @Volatile var lastProjMatrix = FloatArray(16)
    @Volatile var lastViewMatrix = FloatArray(16)
    @Volatile var viewportWidth = 1
    @Volatile var viewportHeight = 1

    var onFrameStats: ((fps: Int, objectCount: Int, vertexCount: Int) -> Unit)? = null
    private var frameCount = 0
    private var lastStatsTime = System.nanoTime()

    private var gridProgram = 0
    private var gridVbo = 0
    private var gridVertexCount = 0
    private var gridUMvp = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.055f, 0.063f, 0.075f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        program = buildProgram(Shaders.VERTEX_SHADER, Shaders.FRAGMENT_SHADER)
        uMVP = GLES30.glGetUniformLocation(program, "uMVP")
        uModel = GLES30.glGetUniformLocation(program, "uModel")
        uNormalMatrix = GLES30.glGetUniformLocation(program, "uNormalMatrix")
        uColor = GLES30.glGetUniformLocation(program, "uColor")
        uLightDir = GLES30.glGetUniformLocation(program, "uLightDir")
        uCameraPos = GLES30.glGetUniformLocation(program, "uCameraPos")
        uSelected = GLES30.glGetUniformLocation(program, "uSelected")

        meshes[PrimitiveType.CUBE] = MeshFactory.cube().also { it.uploadToGpu() }
        meshes[PrimitiveType.SPHERE] = MeshFactory.sphere().also { it.uploadToGpu() }
        meshes[PrimitiveType.CYLINDER] = MeshFactory.cylinder().also { it.uploadToGpu() }

        setupGrid()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 55f, aspect, 0.05f, 500f)
        System.arraycopy(projMatrix, 0, lastProjMatrix, 0, 16)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        camera.computeViewMatrix(viewMatrix)
        System.arraycopy(viewMatrix, 0, lastViewMatrix, 0, 16)

        drawGrid()

        GLES30.glUseProgram(program)
        val eye = camera.eye
        GLES30.glUniform3f(uLightDir, -0.4f, -1f, -0.5f)
        GLES30.glUniform3f(uCameraPos, eye[0], eye[1], eye[2])

        var vertexCount = 0
        for (obj in scene.objects) {
            if (!obj.visible) continue
            val mesh = meshes[obj.type] ?: continue
            obj.transform.toModelMatrix(modelMatrix)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0)

            Matrix.invertM(normalMatrix4, 0, modelMatrix, 0)
            Matrix.transposeM(normalMatrix4, 0, normalMatrix4, 0)
            normalMatrix3[0] = normalMatrix4[0]; normalMatrix3[1] = normalMatrix4[1]; normalMatrix3[2] = normalMatrix4[2]
            normalMatrix3[3] = normalMatrix4[4]; normalMatrix3[4] = normalMatrix4[5]; normalMatrix3[5] = normalMatrix4[6]
            normalMatrix3[6] = normalMatrix4[8]; normalMatrix3[7] = normalMatrix4[9]; normalMatrix3[8] = normalMatrix4[10]

            GLES30.glUniformMatrix4fv(uMVP, 1, false, mvpMatrix, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
            GLES30.glUniformMatrix3fv(uNormalMatrix, 1, false, normalMatrix3, 0)
            GLES30.glUniform4fv(uColor, 1, obj.color, 0)
            GLES30.glUniform1f(uSelected, if (obj.id == scene.selectedId) 1f else 0f)

            drawMesh(mesh)
            vertexCount += mesh.indexCount
        }

        frameCount++
        val now = System.nanoTime()
        if (now - lastStatsTime > 1_000_000_000L) {
            onFrameStats?.invoke(frameCount, scene.objects.size, vertexCount)
            frameCount = 0
            lastStatsTime = now
        }
    }

    private fun drawMesh(mesh: Mesh) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.vbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.nbo)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.ibo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    // --- Ground grid -------------------------------------------------------

    private fun setupGrid(halfSize: Int = 20) {
        val gridVertexShader = """
            #version 300 es
            uniform mat4 uMvp;
            layout(location = 0) in vec3 aPosition;
            void main() { gl_Position = uMvp * vec4(aPosition, 1.0); }
        """
        val gridFragmentShader = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            void main() { fragColor = vec4(0.22, 0.25, 0.28, 0.6); }
        """
        gridProgram = buildProgram(gridVertexShader, gridFragmentShader)
        gridUMvp = GLES30.glGetUniformLocation(gridProgram, "uMvp")

        val lines = ArrayList<Float>()
        for (i in -halfSize..halfSize) {
            lines.add(i.toFloat()); lines.add(0f); lines.add(-halfSize.toFloat())
            lines.add(i.toFloat()); lines.add(0f); lines.add(halfSize.toFloat())
            lines.add(-halfSize.toFloat()); lines.add(0f); lines.add(i.toFloat())
            lines.add(halfSize.toFloat()); lines.add(0f); lines.add(i.toFloat())
        }
        gridVertexCount = lines.size / 3
        val buf: FloatBuffer = ByteBuffer.allocateDirect(lines.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                lines.forEach { put(it) }
                position(0)
            }
        val handle = IntArray(1)
        GLES30.glGenBuffers(1, handle, 0)
        gridVbo = handle[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, lines.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun drawGrid() {
        GLES30.glUseProgram(gridProgram)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, FloatArray(16).also { Matrix.setIdentityM(it, 0) }, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(gridUMvp, 1, false, mvpMatrix, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, gridVertexCount)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    // --- Shader helpers ------------------------------------------------------

    private fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        val status = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(prog)
            GLES30.glDeleteProgram(prog)
            throw RuntimeException("Program link failed: $log")
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }
}
