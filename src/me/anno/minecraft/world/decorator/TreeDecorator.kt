package me.anno.minecraft.world.decorator

import me.anno.minecraft.block.BlockRegistry.Grass
import me.anno.minecraft.block.BlockRegistry.Leaves
import me.anno.minecraft.block.BlockRegistry.Log
import me.anno.minecraft.world.Chunk
import org.joml.Vector3i

class TreeDecorator(density: Float = 0.1f, seed: Long = 5123L) :
    SurfaceDecorator(Grass, Vector3i(2, 6, 2), density, seed) {

    // todo general shape templates for easier programming
    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        val stump = 4
        // place crone
        val cy = ly + stump
        for (dz in -2..2) {
            for (dx in -2..2) {
                chunk.addBlockWithin(lx + dx, cy - 1, lz + dz, Leaves)
                chunk.addBlockWithin(lx + dx, cy, lz + dz, Leaves)
            }
        }
        for (dz in -1..1) {
            for (dx in -1..1) {
                chunk.addBlockWithin(lx + dx, cy + 1, lz + dz, Leaves)
            }
        }
        chunk.addBlockWithin(lx, cy + 2, lz, Leaves)
        chunk.addBlockWithin(lx + 1, cy + 2, lz, Leaves)
        chunk.addBlockWithin(lx - 1, cy + 2, lz, Leaves)
        chunk.addBlockWithin(lx, cy + 2, lz + 1, Leaves)
        chunk.addBlockWithin(lx, cy + 2, lz - 1, Leaves)
        // place stump
        for (i in 0 until stump) {
            chunk.addBlockWithin(lx, ly + i, lz, Log)
        }
    }

}