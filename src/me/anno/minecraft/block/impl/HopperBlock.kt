package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.builder.BlockBuilder
import me.anno.minecraft.block.builder.DetailedBlockMesh16
import me.anno.minecraft.block.types.DetailedBlockVisuals

class HopperBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockWithInventory(typeUUID, color, texId, nameDesc, 5), DetailedBlockVisuals {

    companion object {
        private val texId = 3 * 16 + 13
        private val model = BlockBuilder()
            .addCube(0, 12, 0, 16, 4, 16, texId)
            .addCube(2, 8, 2, 12, 4, 12, texId)
            .addCube(4, 4, 4, 8, 4, 8, texId)
            .addCube(6, 0, 6, 4, 4, 4, texId)
            .build()
    }

    override fun getModel(): DetailedBlockMesh16 = model

    // todo process items
    // todo custom UI

}