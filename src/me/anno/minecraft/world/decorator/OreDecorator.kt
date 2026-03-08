package me.anno.minecraft.world.decorator

import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.world.Chunk
import org.joml.Vector3i

class OreDecorator(density: Float = 0.1f, val blockType: BlockType, seed: Long = 5123L) :
    NNNDecorator(density, Vector3i(3), seed) {

    // todo general shape templates for easier programming
    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        // todo generate sphere shape...
        if (chunk.getBlock(lx, ly, lz) == BlockRegistry.Stone) {
            chunk.setBlockQuickly(lx, ly, lz, blockType)
        }
    }

}