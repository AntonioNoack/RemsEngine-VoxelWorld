package me.anno.minecraft.item

import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc

open class ItemType(
    val texture: FileReference, val texId: Int,
    val nameDesc: NameDesc
) {

}