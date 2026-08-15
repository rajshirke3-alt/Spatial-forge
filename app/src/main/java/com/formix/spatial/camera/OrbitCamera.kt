package com.formix.spatial.camera

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class OrbitCamera {
    var targetX = 0f
    var targetY = 0f
    var targetZ = 0f

    var yawDeg = 40f
    var pitchDeg = 25f
    var distance = 6f

    private val minDistance = 1.2f
    private val maxDistance = 60f
    private val minPitch = -85f
    private val maxPitch = 85f

    val eye = FloatArray(3)

    fun orbit(deltaYawDeg: Float, deltaPitchDeg: Float) {
        yawDeg -= deltaYawDeg
        pitchDeg = max(minPitch, min(maxPitch, pitchDeg - deltaPitchDeg))
    }

    fun zoomBy(factor: Float) {
        distance = max(minDistance, min(maxDistance, distance * factor))
    }

    fun panBy(dx: Float, dy: Float) {
        // Pan along camera-relative right/up vectors, scaled by distance so it feels consistent when zoomed.
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val rightX = cos(yawRad).toFloat()
        val rightZ = -sin(yawRad).toFloat()
        val scale = distance * 0.0015f
        targetX += (-dx * rightX) * scale
        targetZ += (-dx * rightZ) * scale
        targetY += dy * scale
    }

    fun computeEye(): FloatArray {
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val pitchRad = Math.toRadians(pitchDeg.toDouble())
        val cosP = cos(pitchRad).toFloat()
        eye[0] = targetX + distance * cosP * sin(yawRad).toFloat()
        eye[1] = targetY + distance * sin(pitchRad).toFloat()
        eye[2] = targetZ + distance * cosP * cos(yawRad).toFloat()
        return eye
    }

    fun computeViewMatrix(out: FloatArray) {
        val e = computeEye()
        Matrix.setLookAtM(out, 0, e[0], e[1], e[2], targetX, targetY, targetZ, 0f, 1f, 0f)
    }

    /** Camera-facing right/up unit vectors, used for screen-plane object dragging. */
    fun rightVector(): FloatArray {
        val yawRad = Math.toRadians(yawDeg.toDouble())
        return floatArrayOf(cos(yawRad).toFloat(), 0f, -sin(yawRad).toFloat())
    }

    fun upVector(): FloatArray = floatArrayOf(0f, 1f, 0f)

    fun frameAll(radius: Float) {
        distance = max(minDistance, min(maxDistance, radius * 2.6f))
    }
}
