package me.anno.remcraft.world.decorator.underground

import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.world.Chunk
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object CaveUtils {
    fun carveSphere(chunk: Chunk, cx: Float, cy: Float, cz: Float, r: Float) {
        val rSq = r * r

        val x0 = max(ceil(cx - r).toInt(), 0)
        val y0 = max(ceil(cy - r).toInt(), 0)
        val z0 = max(ceil(cz - r).toInt(), 0)

        val x1 = min(ceil(cx + r).toInt(), dimension.sizeX)
        val y1 = min(ceil(cy + r).toInt(), dimension.sizeY)
        val z1 = min(ceil(cz + r).toInt(), dimension.sizeZ)
        if (x0 >= x1 || y0 >= y1 || z0 >= z1) return // quick-path

        val air = BlockRegistry.Air
        for (x in x0 until x1) {
            for (y in y0 until y1) {
                for (z in z0 until z1) {

                    val dx = x - cx
                    val dy = y - cy
                    val dz = z - cz
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        chunk.setBlock(x, y, z, air)
                    }
                }
            }
        }
    }

    fun carveEllipsoid(
        chunk: Chunk,
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float
    ) {
        val irx = 1f / rx
        val iry = 1f / ry
        val irz = 1f / rz

        val x0 = max(ceil(cx - rx).toInt(), 0)
        val y0 = max(ceil(cy - ry).toInt(), 0)
        val z0 = max(ceil(cz - rz).toInt(), 0)

        val x1 = min(ceil(cx + rx).toInt(), dimension.sizeX)
        val y1 = min(ceil(cy + ry).toInt(), dimension.sizeY)
        val z1 = min(ceil(cz + rz).toInt(), dimension.sizeZ)
        if (x0 >= x1 || y0 >= y1 || z0 >= z1) return // quick-path

        val air = BlockRegistry.Air
        for (x in x0 until x1) {
            for (y in y0 until y1) {
                for (z in z0 until z1) {

                    val dx = x - cx
                    val dy = y - cy
                    val dz = z - cz

                    val nx = dx * irx
                    val ny = dy * iry
                    val nz = dz * irz

                    if (nx * nx + ny * ny + nz * nz <= 1f) {
                        chunk.setBlock(x, y, z, air)
                    }
                }
            }
        }
    }
}