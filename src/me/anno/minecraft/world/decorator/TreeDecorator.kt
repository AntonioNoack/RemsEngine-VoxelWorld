package me.anno.minecraft.world.decorator

import me.anno.minecraft.block.BlockType
import me.anno.minecraft.world.Chunk
import org.joml.Vector3i

class TreeDecorator(density: Float = 0.1f, seed: Long = 5123L) :
    SurfaceDecorator(BlockType.Grass, Vector3i(2, 6, 2), density, seed) {

    // todo general shape templates for easier programming
    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        val stump = 4
        // place crone
        val cy = ly + stump
        for (dz in -2..2) {
            for (dx in -2..2) {
                chunk.addBlockWithin(lx + dx, cy - 1, lz + dz, BlockType.Leaves)
                chunk.addBlockWithin(lx + dx, cy, lz + dz, BlockType.Leaves)
            }
        }
        for (dz in -1..1) {
            for (dx in -1..1) {
                chunk.addBlockWithin(lx + dx, cy + 1, lz + dz, BlockType.Leaves)
            }
        }
        chunk.addBlockWithin(lx, cy + 2, lz, BlockType.Leaves)
        chunk.addBlockWithin(lx + 1, cy + 2, lz, BlockType.Leaves)
        chunk.addBlockWithin(lx - 1, cy + 2, lz, BlockType.Leaves)
        chunk.addBlockWithin(lx, cy + 2, lz + 1, BlockType.Leaves)
        chunk.addBlockWithin(lx, cy + 2, lz - 1, BlockType.Leaves)
        // place stump
        for (i in 0 until stump) {
            chunk.addBlockWithin(lx, ly + i, lz, BlockType.Log)
        }
    }

}