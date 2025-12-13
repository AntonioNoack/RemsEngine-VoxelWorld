package me.anno.minecraft.block

import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.minecraft.item.ItemType
import me.anno.utils.Color.a
import org.joml.AABBd
import org.joml.AABBf

open class BlockType(typeUUID: String, val color: Int, texId: Int, nameDesc: NameDesc) :
    ItemType(typeUUID, InvalidRef, texId, nameDesc) {

    companion object {
        private val defaultBlockSize = AABBf(0f, 0f, 0f, 1f, 1f, 1f)
    }

    var id: Short = -1

    val isSolid get() = color.a() == 255
    val isFluid get() = color.a() in 1 until 255
    val isWalkable get() = !isSolid && !isFluid
    val isGrassy = "grass" in typeUUID

    var friction = 5f

    override fun toString(): String {
        return nameDesc.name
    }

    fun getBounds(x: Int, y: Int, z: Int,  dst: AABBd): AABBd {
        val cx = x.toDouble()
        val cy = y.toDouble()
        val cz = z.toDouble()
        val size = (this as? CustomBlockBounds)?.customSize ?: defaultBlockSize
        return dst.set(size).translate(cx, cy, cz)
    }

}