package me.anno.minecraft.block

import me.anno.minecraft.block.builder.DetailedBlockMesh16

interface DetailedBlockVisuals {
    fun getModel(): DetailedBlockMesh16
}