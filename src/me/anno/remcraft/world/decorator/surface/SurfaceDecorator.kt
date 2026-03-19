package me.anno.remcraft.world.decorator.surface

import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.decorator.N1NDecorator
import org.joml.Vector3i

abstract class SurfaceDecorator(
    val surfaceBlock: BlockType,
    maxExtends: Vector3i, density: Float, seed: Long
) : N1NDecorator(density, maxExtends, seed) {

    override fun decorate(chunk: Chunk, lx: Int, lz: Int) {
        // find the surface y
        // it might be +1/-1 from us...
        val dim = chunk.dimension
        val y0 = chunk.y0
        val gx = chunk.x0 + lx
        val gz = chunk.z0 + lz

        val maxDy = maxExtends.y + sizeY

        if (dim.getBlockAt(gx, y0 + maxDy, gz, chunk.stage) == BlockRegistry.Air) {
            // there is sky -> we can generate something
            for (y in y0 + maxDy downTo y0 - maxExtends.y) {
                val block = dim.getBlockAt(gx, y, gz, chunk.stage)
                if (block != BlockRegistry.Air) {
                    if (block == surfaceBlock) {
                        // we found the start
                        decorate(chunk, lx, y - y0 + 1, lz)
                        return
                    }
                }
            }
        }
    }

    /**
     * decorate thing on surface
     * @param lx local x coordinate
     * @param ly local y coordinate of first air block
     * @param lz local z coordinate
     * */
    abstract fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int)

}