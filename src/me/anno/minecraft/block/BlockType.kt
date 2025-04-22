package me.anno.minecraft.block

import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.minecraft.item.ItemType
import me.anno.utils.Color.a

open class BlockType(typeUUID: String, val color: Int, texId: Int, nameDesc: NameDesc) :
    ItemType(typeUUID, InvalidRef, texId, nameDesc) {

    var id: Short = -1

    val isSolid get() = color.a() == 255
    val isFluid get() = color.a() in 1 until 255

    override fun toString(): String {
        return nameDesc.name
    }

}