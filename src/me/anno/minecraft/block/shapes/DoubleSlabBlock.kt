package me.anno.minecraft.block.shapes

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.BlockType

class DoubleSlabBlock(val blockType: BlockType) : BlockType(
    "${blockType.typeUUID}.doubleSlab", blockType.color, blockType.texId,
    NameDesc("${blockType.nameDesc.name} Double-Slab")
)