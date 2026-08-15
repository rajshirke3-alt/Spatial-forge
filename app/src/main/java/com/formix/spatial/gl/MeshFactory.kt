package com.formix.spatial.gl

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Builds unit-sized primitive meshes (~1 world unit across / tall). Object [Transform.scale]
 * is used to size individual instances so a single Mesh per type can be shared/cached.
 */
object MeshFactory {

    fun cube(): Mesh {
        val h = 0.5f
        // 24 verts (4 per face) so each face gets flat-shaded normals
        val facePositions = arrayOf(
            // +X
            floatArrayOf(h, -h, -h, h, h, -h, h, h, h, h, -h, h), floatArrayOf(1f, 0f, 0f),
            // -X
            floatArrayOf(-h, -h, h, -h, h, h, -h, h, -h, -h, -h, -h), floatArrayOf(-1f, 0f, 0f),
            // +Y
            floatArrayOf(-h, h, -h, -h, h, h, h, h, h, h, h, -h), floatArrayOf(0f, 1f, 0f),
            // -Y
            floatArrayOf(-h, -h, h, -h, -h, -h, h, -h, -h, h, -h, h), floatArrayOf(0f, -1f, 0f),
            // +Z
            floatArrayOf(-h, -h, h, h, -h, h, h, h, h, -h, h, h), floatArrayOf(0f, 0f, 1f),
            // -Z
            floatArrayOf(h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h), floatArrayOf(0f, 0f, -1f)
        )
        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val indices = ArrayList<Short>()
        var faceIdx = 0
        var vertBase: Short = 0
        var i = 0
        while (i < facePositions.size) {
            val verts = facePositions[i] as FloatArray
            val n = facePositions[i + 1] as FloatArray
            for (v in 0 until 4) {
                positions.add(verts[v * 3]); positions.add(verts[v * 3 + 1]); positions.add(verts[v * 3 + 2])
                normals.add(n[0]); normals.add(n[1]); normals.add(n[2])
            }
            indices.add(vertBase); indices.add((vertBase + 1).toShort()); indices.add((vertBase + 2).toShort())
            indices.add(vertBase); indices.add((vertBase + 2).toShort()); indices.add((vertBase + 3).toShort())
            vertBase = (vertBase + 4).toShort()
            i += 2
            faceIdx++
        }
        return Mesh(positions.toFloatArray(), normals.toFloatArray(), indices.toShortArray())
    }

    fun sphere(stacks: Int = 16, slices: Int = 24): Mesh {
        val r = 0.5f
        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val indices = ArrayList<Short>()

        for (stack in 0..stacks) {
            val phi = PI * stack / stacks // 0..PI
            val y = cos(phi).toFloat()
            val ringR = sin(phi).toFloat()
            for (slice in 0..slices) {
                val theta = 2.0 * PI * slice / slices
                val x = (ringR * cos(theta)).toFloat()
                val z = (ringR * sin(theta)).toFloat()
                positions.add(x * r); positions.add(y * r); positions.add(z * r)
                normals.add(x); normals.add(y); normals.add(z)
            }
        }
        val ringVerts = slices + 1
        for (stack in 0 until stacks) {
            for (slice in 0 until slices) {
                val a = (stack * ringVerts + slice)
                val b = (a + ringVerts)
                indices.add(a.toShort()); indices.add(b.toShort()); indices.add((a + 1).toShort())
                indices.add((a + 1).toShort()); indices.add(b.toShort()); indices.add((b + 1).toShort())
            }
        }
        return Mesh(positions.toFloatArray(), normals.toFloatArray(), indices.toShortArray())
    }

    fun cylinder(segments: Int = 24): Mesh {
        val r = 0.5f
        val h = 0.5f
        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val indices = ArrayList<Short>()

        // Side wall: two rings (top/bottom) with outward normals
        for (ring in 0..1) {
            val y = if (ring == 0) -h else h
            for (s in 0..segments) {
                val theta = 2.0 * PI * s / segments
                val x = cos(theta).toFloat()
                val z = sin(theta).toFloat()
                positions.add(x * r); positions.add(y); positions.add(z * r)
                normals.add(x); normals.add(0f); normals.add(z)
            }
        }
        val ringVerts = segments + 1
        for (s in 0 until segments) {
            val a = s
            val b = a + ringVerts
            indices.add(a.toShort()); indices.add(b.toShort()); indices.add((a + 1).toShort())
            indices.add((a + 1).toShort()); indices.add(b.toShort()); indices.add((b + 1).toShort())
        }

        // Bottom cap
        val bottomCenterIdx = positions.size / 3
        positions.add(0f); positions.add(-h); positions.add(0f)
        normals.add(0f); normals.add(-1f); normals.add(0f)
        val bottomStart = positions.size / 3
        for (s in 0..segments) {
            val theta = 2.0 * PI * s / segments
            positions.add(cos(theta).toFloat() * r); positions.add(-h); positions.add(sin(theta).toFloat() * r)
            normals.add(0f); normals.add(-1f); normals.add(0f)
        }
        for (s in 0 until segments) {
            indices.add(bottomCenterIdx.toShort())
            indices.add((bottomStart + s + 1).toShort())
            indices.add((bottomStart + s).toShort())
        }

        // Top cap
        val topCenterIdx = positions.size / 3
        positions.add(0f); positions.add(h); positions.add(0f)
        normals.add(0f); normals.add(1f); normals.add(0f)
        val topStart = positions.size / 3
        for (s in 0..segments) {
            val theta = 2.0 * PI * s / segments
            positions.add(cos(theta).toFloat() * r); positions.add(h); positions.add(sin(theta).toFloat() * r)
            normals.add(0f); normals.add(1f); normals.add(0f)
        }
        for (s in 0 until segments) {
            indices.add(topCenterIdx.toShort())
            indices.add((topStart + s).toShort())
            indices.add((topStart + s + 1).toShort())
        }

        return Mesh(positions.toFloatArray(), normals.toFloatArray(), indices.toShortArray())
    }
}
