package me.anno.remcraft.block.types

import me.anno.remcraft.block.Metadata
import me.anno.remcraft.world.Chunk

interface ChangingBlock {
    fun onBlockUpdate(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk)
}