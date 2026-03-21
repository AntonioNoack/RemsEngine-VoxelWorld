package me.anno.remcraft.world.decorator.surface

import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import org.joml.Vector3i
import kotlin.math.max
import kotlin.math.min

class PyramidDecorator(
    val material: BlockType,
    val halfSize: Int,
    surfaceBlock: BlockType,
    density: Float, seed: Long
) : SurfaceDecorator(surfaceBlock, Vector3i(halfSize + 10, halfSize + 10, halfSize + 10), density, seed) {

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        val halfSize = halfSize
        val material = material
        val y0 = max(-10, -ly)
        val y1 = min(halfSize, sizeY - ly)
        for (dy in y0 until y1) {
            val y = ly + dy
            val radius = halfSize - 1 - dy
            val x0 = max(lx - radius, 0)
            val z0 = max(lz - radius, 0)
            val x1 = min(lx + radius, sizeX - 1)
            val z1 = min(lz + radius, sizeZ - 1)
            if (x0 > x1 || z0 > z1) continue // fast-path

            for (x in x0..x1) {
                for (z in z0..z1) {
                    chunk.setBlockIfAir(x, y, z, material)
                }
            }
        }
    }

}