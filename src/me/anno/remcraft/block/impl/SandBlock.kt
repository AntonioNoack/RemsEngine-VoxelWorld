package me.anno.remcraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.Metadata
import me.anno.remcraft.block.types.ChangingBlock
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.world.Chunk

class SandBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), ChangingBlock {

    override fun onBlockUpdate(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk) {
        val belowChunk = dimension.getChunkAt(x, y - 1, z) ?: return
        if (!belowChunk.getBlock(x, y - 1, z).isSolid) {
            startFalling(x, y, z, metadata)
        }
    }
}