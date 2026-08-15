package com.formix.spatial.input

import android.view.MotionEvent
import android.view.View
import com.formix.spatial.camera.OrbitCamera
import com.formix.spatial.gl.SceneRenderer
import com.formix.spatial.history.TransformCommand
import com.formix.spatial.scene.Scene
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Handles: one-finger orbit (Navigate tool) or transform-drag (Move/Rotate/Scale tools),
 * two-finger pan, pinch zoom, and tap-to-select (always active, any tool).
 */
class GestureController(
    private val scene: Scene,
    private val camera: OrbitCamera,
    private val renderer: SceneRenderer
) : View.OnTouchListener {

    var toolMode: ToolMode = ToolMode.NAVIGATE
    var onSelectionChanged: (() -> Unit)? = null
    var onTransformCommitted: ((TransformCommand) -> Unit)? = null

    private var prevX = 0f
    private var prevY = 0f
    private var pointerCount = 0

    private var prevPinchDistance = 0f
    private var prevMidX = 0f
    private var prevMidY = 0f

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var movedDistance = 0f

    private var dragStartTransform: com.formix.spatial.scene.Transform? = null

    private val tapSlopPx = 24f
    private val tapTimeoutMs = 300L

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x; prevY = event.y
                downX = event.x; downY = event.y
                downTime = System.currentTimeMillis()
                movedDistance = 0f
                pointerCount = 1

                val selected = scene.selected()
                dragStartTransform = if (toolMode != ToolMode.NAVIGATE && selected != null) {
                    selected.transform.copy2()
                } else null
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    pointerCount = 2
                    prevPinchDistance = pinchDistance(event)
                    prevMidX = midX(event); prevMidY = midY(event)
                    // A second finger cancels any single-finger transform drag in progress.
                    dragStartTransform = null
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount >= 2 && event.pointerCount >= 2) {
                    val dist = pinchDistance(event)
                    if (prevPinchDistance > 1f) {
                        val factor = prevPinchDistance / dist
                        camera.zoomBy(factor)
                    }
                    val mx = midX(event); val my = midY(event)
                    camera.panBy(mx - prevMidX, my - prevMidY)
                    prevPinchDistance = dist
                    prevMidX = mx; prevMidY = my
                } else if (pointerCount == 1 && event.pointerCount == 1) {
                    val dx = event.x - prevX
                    val dy = event.y - prevY
                    movedDistance += abs(dx) + abs(dy)

                    when (toolMode) {
                        ToolMode.NAVIGATE -> camera.orbit(dx * 0.35f, dy * 0.35f)
                        ToolMode.MOVE -> applyMove(dx, dy)
                        ToolMode.ROTATE -> applyRotate(dx, dy)
                        ToolMode.SCALE -> applyScale(dy)
                    }
                    prevX = event.x; prevY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Drop back to single-finger tracking using whichever pointer remains.
                pointerCount = 1
                val idx = if (event.actionIndex == 0) 1 else 0
                if (idx < event.pointerCount) {
                    prevX = event.getX(idx); prevY = event.getY(idx)
                }
            }

            MotionEvent.ACTION_UP -> {
                val elapsed = System.currentTimeMillis() - downTime
                if (movedDistance < tapSlopPx && elapsed < tapTimeoutMs) {
                    val hit = RayPicker.pick(event.x, event.y, renderer, camera, scene)
                    scene.selectedId = hit?.id
                    onSelectionChanged?.invoke()
                } else {
                    val start = dragStartTransform
                    val obj = scene.selected()
                    if (start != null && obj != null) {
                        val after = obj.transform.copy2()
                        // Object is already at 'after' visually; wrap in a command so undo/redo work.
                        onTransformCommitted?.invoke(TransformCommand(scene, obj.id, start, after))
                    }
                }
                dragStartTransform = null
                pointerCount = 0
            }

            MotionEvent.ACTION_CANCEL -> {
                dragStartTransform = null
                pointerCount = 0
            }
        }
        return true
    }

    private fun applyMove(dx: Float, dy: Float) {
        val obj = scene.selected() ?: return
        val right = camera.rightVector()
        val up = camera.upVector()
        val scale = camera.distance * 0.0022f
        obj.transform.px += (right[0] * dx - up[0] * dy) * scale
        obj.transform.py += (right[1] * dx - up[1] * dy) * scale
        obj.transform.pz += (right[2] * dx - up[2] * dy) * scale
    }

    private fun applyRotate(dx: Float, dy: Float) {
        val obj = scene.selected() ?: return
        obj.transform.ry += dx * 0.5f
        obj.transform.rx += dy * 0.5f
    }

    private fun applyScale(dy: Float) {
        val obj = scene.selected() ?: return
        val factor = 1f - dy * 0.004f
        val t = obj.transform
        t.sx = (t.sx * factor).coerceIn(0.05f, 40f)
        t.sy = (t.sy * factor).coerceIn(0.05f, 40f)
        t.sz = (t.sz * factor).coerceIn(0.05f, 40f)
    }

    private fun pinchDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx, dy)
    }

    private fun midX(event: MotionEvent) = (event.getX(0) + event.getX(1)) / 2f
    private fun midY(event: MotionEvent) = (event.getY(0) + event.getY(1)) / 2f
}
