package me.anno.minecraft.entity.model

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY

object PlaneCreator {
    fun createPlane(
        sx: Int, sy: Int,
        x0: Int, y0: Int,
        textureSize: Int
    ): Mesh {
        val invX = 1f / getSizeX(textureSize)
        val invY = 1f / getSizeY(textureSize)
        return createPlane(
            x0 * invX, y0 * invY,
            (x0 + sx) * invX, (y0 + sy) * invY,
        )
    }

    fun createPlane(
        u0: Float, v0: Float,
        u1: Float, v1: Float,
    ): Mesh = Mesh().apply {
        val halfSize = 0.5f
        positions = floatArrayOf(
            -halfSize, -halfSize, 0f,
            -halfSize, +halfSize, 0f,
            +halfSize, -halfSize, 0f,
            +halfSize, +halfSize, 0f,
        )
        uvs = floatArrayOf(
            u0, v0,
            u0, v1,
            u1, v0,
            u1, v1
        )
        normals = floatArrayOf(
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
        )
        // both-sided
        indices = intArrayOf(
            0, 1, 3, 0, 3, 1,
            0, 3, 2, 0, 2, 3,
        )
    }
}