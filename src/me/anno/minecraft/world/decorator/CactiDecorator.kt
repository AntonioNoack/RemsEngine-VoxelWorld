package me.anno.minecraft.world.decorator

import me.anno.minecraft.block.BlockType
import me.anno.minecraft.world.Chunk
import org.joml.Vector3i

class CactiDecorator(density: Float = 0.1f, seed: Long = 5123L) :
    SurfaceDecorator(BlockType.Sand, Vector3i(0, 2, 0), density, seed) {

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        // place stump
        for (i in 0 until 3) {
            chunk.addBlockWithin(lx, ly + i, lz, BlockType.Cactus)
        }
    }

}