package me.anno.remcraft.world.decorator.surface

import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.world.Chunk
import org.joml.Vector3i

class CactiDecorator(density: Float = 0.1f, seed: Long = 5123L) :
    SurfaceDecorator(BlockRegistry.Sand, Vector3i(0, 2, 0), density, seed) {

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        // place stump
        for (i in 0 until 3) {
            chunk.setBlockIfAir(lx, ly + i, lz, BlockRegistry.Cactus)
        }
    }

}