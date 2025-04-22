package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.mesh.vox.meshing.BlockSide

class SlabBlock(val blockType: BlockType, val side: BlockSide) :
    BlockType(
        "${blockType.typeUUID}.slab[${side.id}]", blockType.color,
        blockType.texId, NameDesc("${blockType.nameDesc.name} Slab")
    ) {


}