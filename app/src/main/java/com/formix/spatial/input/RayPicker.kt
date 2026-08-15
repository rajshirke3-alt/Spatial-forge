package com.formix.spatial.input

import android.opengl.GLU
import com.formix.spatial.camera.OrbitCamera
import com.formix.spatial.gl.SceneRenderer
import com.formix.spatial.scene.Scene
import com.formix.spatial.scene.SceneObject
import kotlin.math.sqrt

object RayPicker {

    /**
     * Unprojects the screen tap into a world-space ray and returns the nearest
     * scene object whose bounding sphere the ray intersects, or null.
     */
    fun pick(
        screenX: Float,
        screenY: Float,
        renderer: SceneRenderer,
        camera: OrbitCamera,
        scene: Scene
    ): SceneObject? {
        val viewport = intArrayOf(0, 0, renderer.viewportWidth, renderer.viewportHeight)
        val glY = renderer.viewportHeight - screenY // GL has origin bottom-left

        val nearPoint = FloatArray(4)
        val farPoint = FloatArray(4)
        val ok1 = GLU.gluUnProject(
            screenX, glY, 0f, renderer.lastViewMatrix, 0, renderer.lastProjMatrix, 0, viewport, 0, nearPoint, 0
        )
        val ok2 = GLU.gluUnProject(
            screenX, glY, 1f, renderer.lastViewMatrix, 0, renderer.lastProjMatrix, 0, viewport, 0, farPoint, 0
        )
        if (ok1 != android.opengl.GLES10.GL_TRUE || ok2 != android.opengl.GLES10.GL_TRUE) return null

        val ox = nearPoint[0] / nearPoint[3]; val oy = nearPoint[1] / nearPoint[3]; val oz = nearPoint[2] / nearPoint[3]
        var dx = farPoint[0] / farPoint[3] - ox
        var dy = farPoint[1] / farPoint[3] - oy
        var dz = farPoint[2] / farPoint[3] - oz
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-6f) return null
        dx /= len; dy /= len; dz /= len

        var best: SceneObject? = null
        var bestT = Float.MAX_VALUE

        for (obj in scene.objects) {
            if (!obj.visible) continue
            val cx = obj.transform.px; val cy = obj.transform.py; val cz = obj.transform.pz
            val radius = obj.transform.boundingRadius()

            val lx = cx - ox; val ly = cy - oy; val lz = cz - oz
            val tca = lx * dx + ly * dy + lz * dz
            if (tca < 0) continue
            val d2 = (lx * lx + ly * ly + lz * lz) - tca * tca
            val r2 = radius * radius
            if (d2 > r2) continue
            val thc = sqrt(r2 - d2)
            val t0 = tca - thc
            if (t0 in 0f..bestT) {
                bestT = t0
                best = obj
            }
        }
        return best
    }
}
