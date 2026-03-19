package me.anno.remcraft.world.decorator.underground

import me.anno.maths.Maths.sq
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object CaveUtils {
    fun carveSphere(chunk: Chunk, cx: Float, cy: Float, cz: Float, r: Float) {
        val rQuad = r * r * r * r

        val x0 = max(ceil(cx - r).toInt(), 0)
        val y0 = max(ceil(cy - r).toInt(), 0)
        val z0 = max(ceil(cz - r).toInt(), 0)

        val x1 = min(ceil(cx + r).toInt(), sizeX)
        val y1 = min(ceil(cy + r).toInt(), sizeY)
        val z1 = min(ceil(cz + r).toInt(), sizeZ)
        if (x0 >= x1 || y0 >= y1 || z0 >= z1) return // quick-path

        val air = BlockRegistry.Air.id
        for (x in x0 until x1) {
            for (y in y0 until y1) {
                for (z in z0 until z1) {

                    val dx = x - cx
                    val dy = y - cy
                    val dz = z - cz
                    if (sq(dx * dx + dz * dz) + sq(dy * dy) <= rQuad) {
                        chunk.setBlockQuickly(x, y, z, air)
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

        val x1 = min(ceil(cx + rx).toInt(), sizeX)
        val y1 = min(ceil(cy + ry).toInt(), sizeY)
        val z1 = min(ceil(cz + rz).toInt(), sizeZ)
        if (x0 >= x1 || y0 >= y1 || z0 >= z1) return // quick-path

        val air = BlockRegistry.Air.id
        for (x in x0 until x1) {
            for (y in y0 until y1) {
                for (z in z0 until z1) {

                    val dx = x - cx
                    val dy = y - cy
                    val dz = z - cz

                    val nx = dx * irx
                    val ny = dy * iry
                    val nz = dz * irz

                    if (sq(nx * nx + nz * nz) + sq(ny * ny) <= 1f) {
                        chunk.setBlockQuickly(x, y, z, air)
                    }
                }
            }
        }
    }
}