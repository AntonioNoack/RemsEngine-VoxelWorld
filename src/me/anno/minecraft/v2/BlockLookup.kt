package me.anno.minecraft.v2

import me.anno.minecraft.block.BlockType

fun interface BlockLookup {
    fun lookup(type: BlockType): Int
}