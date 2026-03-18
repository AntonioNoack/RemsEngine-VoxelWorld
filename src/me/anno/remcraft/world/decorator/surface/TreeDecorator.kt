package me.anno.remcraft.world.decorator.surface

import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.world.Chunk
import org.joml.Vector3i

class TreeDecorator(density: Float = 0.1f, seed: Long = 5123L) :
    SurfaceDecorator(BlockRegistry.Grass, Vector3i(2, 6, 2), density, seed) {

    // todo general shape templates for easier programming
    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        val stump = 4
        // place crone
        val cy = ly + stump
        for (dz in -2..2) {
            for (dx in -2..2) {
                chunk.setBlockIfAir(lx + dx, cy - 1, lz + dz, BlockRegistry.Leaves)
                chunk.setBlockIfAir(lx + dx, cy, lz + dz, BlockRegistry.Leaves)
            }
        }
        for (dz in -1..1) {
            for (dx in -1..1) {
                chunk.setBlockIfAir(lx + dx, cy + 1, lz + dz, BlockRegistry.Leaves)
            }
        }
        chunk.setBlockIfAir(lx, cy + 2, lz, BlockRegistry.Leaves)
        chunk.setBlockIfAir(lx + 1, cy + 2, lz, BlockRegistry.Leaves)
        chunk.setBlockIfAir(lx - 1, cy + 2, lz, BlockRegistry.Leaves)
        chunk.setBlockIfAir(lx, cy + 2, lz + 1, BlockRegistry.Leaves)
        chunk.setBlockIfAir(lx, cy + 2, lz - 1, BlockRegistry.Leaves)
        // place stump
        for (i in 0 until stump) {
            chunk.setBlockIfAir(lx, ly + i, lz, BlockRegistry.Log)
        }
    }

}