package me.anno.minecraft.block

import me.anno.minecraft.world.Chunk

interface ChangingBlock {
    fun onBlockUpdate(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk)
}