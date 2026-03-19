package me.anno.remcraft.world.decorator.terrain

import me.anno.remcraft.block.BlockRegistry.Air
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.getIndex
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.decorator.Decorator
import kotlin.math.min

class FlatWorldGenerator(val layers: List<BlockType>) : Decorator() {
    override fun decorate(chunk: Chunk) {
        val blocks = chunk.blocks
        if (chunk.y0 < 0) return

        for (y in 0 until min(sizeY, layers.size - chunk.y0)) {
            val block = layers[y + chunk.y0]
            if (block != Air) {
                val i0 = getIndex(0, y, 0)
                var i1 = getIndex(sizeX - 1, y + 1, sizeZ - 1)
                if (i1 < i0) i1 = blocks.size // could happen at the end
                blocks.fill(block.id, i0, i1)
            }
        }
    }
}