package com.formix.spatial.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * CPU-side geometry description. GPU buffers are created lazily on the GL thread
 * via [uploadToGpu] since this object may be constructed before a GL context exists.
 */
class Mesh(
    private val positions: FloatArray,
    private val normals: FloatArray,
    private val indices: ShortArray
) {
    val indexCount: Int = indices.size

    var vbo = 0
        private set
    var nbo = 0
        private set
    var ibo = 0
        private set

    private var uploaded = false

    fun uploadToGpu() {
        if (uploaded) return

        val posBuffer: FloatBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(positions); position(0) }
        val normBuffer: FloatBuffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(normals); position(0) }
        val idxBuffer: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices); position(0) }

        val handles = IntArray(3)
        GLES30.glGenBuffers(3, handles, 0)
        vbo = handles[0]; nbo = handles[1]; ibo = handles[2]

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, positions.size * 4, posBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normals.size * 4, normBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, idxBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)

        uploaded = true
    }
}
