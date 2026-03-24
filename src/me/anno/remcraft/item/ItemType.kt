package me.anno.remcraft.item

import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.remcraft.block.BlockColor.NUM_TEX_X

open class ItemType(
    val typeUUID: String,
    val texture: FileReference, val texId: Int,
    val nameDesc: NameDesc
) {

    var stackingLimit = 100

    var miningHardness = 1f
    var miningType = MiningType.OTHER

    val texX get() = texId % NUM_TEX_X
    val texY get() = texId / NUM_TEX_X

}