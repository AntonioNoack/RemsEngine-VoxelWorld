package me.anno.minecraft.block.types

import me.anno.minecraft.block.builder.DetailedBlockMesh16

interface DetailedBlockVisuals {
    fun getModel(): DetailedBlockMesh16
}