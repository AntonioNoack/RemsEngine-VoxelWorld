package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc

class FurnaceBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockWithInventory(typeUUID, color, texId, nameDesc, 3) {

    companion object {
        const val INPUT_SLOT = 0
        const val FUEL_SLOT = 1
        const val OUTPUT_SLOT = 2
    }

    // todo create nicer UI
    // todo process items

}