package me.anno.minecraft.block.types

import me.anno.minecraft.block.Metadata
import me.anno.minecraft.world.Chunk

interface ChangingBlock {
    fun onBlockUpdate(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk)
}