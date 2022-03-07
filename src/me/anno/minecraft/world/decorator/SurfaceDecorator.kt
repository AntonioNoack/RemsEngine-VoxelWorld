package me.anno.minecraft.world.decorator

import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.world.Chunk

abstract class SurfaceDecorator(val surfaceBlock: BlockType, density: Float, seed: Long) : N1NDecorator(density, seed) {

    override fun decorate(chunk: Chunk, lx: Int, lz: Int) {
        // find the surface y
        // it might be +1/-1 from us...
        val dim = chunk.dim
        val y0 = chunk.y0
        val gx = chunk.x0 + lx
        val gz = chunk.z0 + lz
        if (dim.getBlockAt(gx, y0 + 2 * dim.sizeY, gz, chunk) == Air) {
            // there is sky -> we can generate something
            for (y in y0 + 2 * dim.sizeY - 1 downTo y0 - dim.sizeY) {
                val block = dim.getBlockAt(gx, y, gz, chunk)
                if (block != Air) {
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