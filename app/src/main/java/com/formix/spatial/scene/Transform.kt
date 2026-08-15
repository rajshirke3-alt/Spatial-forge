package com.formix.spatial.scene

import android.opengl.Matrix

/**
 * Position / rotation (degrees, XYZ euler) / scale for a scene object.
 */
data class Transform(
    var px: Float = 0f, var py: Float = 0f, var pz: Float = 0f,
    var rx: Float = 0f, var ry: Float = 0f, var rz: Float = 0f,
    var sx: Float = 1f, var sy: Float = 1f, var sz: Float = 1f
) {
    fun copy2(): Transform = copy()

    fun toModelMatrix(out: FloatArray) {
        Matrix.setIdentityM(out, 0)
        Matrix.translateM(out, 0, px, py, pz)
        Matrix.rotateM(out, 0, ry, 0f, 1f, 0f)
        Matrix.rotateM(out, 0, rx, 1f, 0f, 0f)
        Matrix.rotateM(out, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(out, 0, sx, sy, sz)
    }

    /** Rough bounding radius in world units, used for ray-pick and framing. Unit primitives are ~1 unit across. */
    fun boundingRadius(): Float {
        val maxScale = maxOf(sx, sy, sz)
        return 0.87f * maxScale // ~ half-diagonal of a unit cube
    }
}
