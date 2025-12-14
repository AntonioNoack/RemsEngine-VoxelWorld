package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.world.Chunk

class SandBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), ChangingBlock {

    override fun onBlockUpdate(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk) {
        val belowChunk = dimension.getChunkAt(x, y - 1, z) ?: return
        if (!belowChunk.getBlock(x, y - 1, z).isSolid) {
            startFalling(x, y, z, metadata)
        }
    }
}