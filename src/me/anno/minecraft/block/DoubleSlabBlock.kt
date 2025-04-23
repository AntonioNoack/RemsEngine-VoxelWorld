package me.anno.minecraft.block

import me.anno.language.translation.NameDesc

class DoubleSlabBlock(val blockType: BlockType) : BlockType(
    "${blockType.typeUUID}.doubleSlab", blockType.color, blockType.texId,
    NameDesc("${blockType.nameDesc.name} Double-Slab")
)