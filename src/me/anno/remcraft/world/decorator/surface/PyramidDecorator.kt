package me.anno.remcraft.world.decorator.surface

import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import org.joml.Vector3i

class PyramidDecorator(
    val material: BlockType,
    val halfSize: Int,
    surfaceBlock: BlockType,
    density: Float, seed: Long
) : SurfaceDecorator(surfaceBlock, Vector3i(halfSize + 10, halfSize + 10, halfSize + 10), density, seed) {

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        val halfSize = halfSize
        val material = material
        for (dy in -10 until halfSize) {
            val y = ly + dy
            val radius = halfSize - 1 - dy
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    chunk.setBlockIfAir(lx + dx, y, lz + dz, material)
                }
            }
        }
    }

}