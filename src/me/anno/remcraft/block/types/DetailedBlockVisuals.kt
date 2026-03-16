package me.anno.remcraft.block.types

import me.anno.remcraft.block.builder.DetailedBlockMesh16

interface DetailedBlockVisuals {
    fun getModel(): DetailedBlockMesh16
}