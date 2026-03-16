package me.anno.remcraft.world.generator

import me.anno.remcraft.block.BlockRegistry.Air
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk

class FlatWorldGenerator(val layers: ArrayList<BlockType>) : Generator() {
    override fun generate(chunk: Chunk) {
        val dim = chunk.dimension
        val blocks = chunk.blocks
        for (y in 0 until dim.sizeY) {
            val block = layers.getOrNull(y + chunk.y0) ?: break
            if (block != Air) {
                val i0 = dim.getIndex(0, y, 0)
                var i1 = dim.getIndex(dim.sizeX - 1, y + 1, dim.sizeZ - 1)
                if (i1 < i0) i1 = blocks.size // could happen at the end
                blocks.fill(block.id, i0, i1)
            }
        }
        loadSaveData(chunk)
    }
}