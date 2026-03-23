package me.anno.remcraft.block

import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.remcraft.audio.playBreakBlockSound
import me.anno.remcraft.audio.playStartFallingSound
import me.anno.remcraft.block.BlockColor.getBlockColor
import me.anno.remcraft.block.types.CustomBlockBounds
import me.anno.remcraft.entity.RemcraftEntity.Companion.spawnEntity
import me.anno.remcraft.entity.ItemEntity
import me.anno.remcraft.entity.MovingBlock
import me.anno.remcraft.entity.XpOrbEntity
import me.anno.remcraft.item.ItemType
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.ui.ItemSlot
import me.anno.utils.Color.a
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3i

open class BlockType(typeUUID: String, val color0: Int, texId: Int, nameDesc: NameDesc) :
    ItemType(typeUUID, InvalidRef, texId, nameDesc) {

    companion object {
        private val defaultBlockSize = AABBf(0f, 0f, 0f, 1f, 1f, 1f)

        fun getDropPosition(x: Int, y: Int, z: Int): Vector3d {
            val center = 0.5
            return Vector3d(x + center, y + center, z + center)
        }

        fun dropXpOrb(x: Int, y: Int, z: Int) {
            val spread = 0.7f
            spawnEntity(
                XpOrbEntity(1),
                getDropPosition(x, y, z).add(
                    (Maths.random().toFloat() - 0.5f) * spread,
                    (Maths.random().toFloat() - 0.5f) * spread,
                    (Maths.random().toFloat() - 0.5f) * spread
                )
            )
        }

        fun dropItem(x: Int, y: Int, z: Int, itemStack: ItemSlot) {
            dropItem(getDropPosition(x, y, z), itemStack)
        }

        fun dropItem(position: Vector3d, itemStack: ItemSlot) {
            val speed = 2f
            spawnEntity(ItemEntity(itemStack), position)
                .physics.setVelocity(
                    (Maths.random().toFloat() - 0.5f) * speed,
                    (Maths.random().toFloat() - 0.5f) * speed,
                    (Maths.random().toFloat() - 0.5f) * speed
                )
        }
    }

    var id: Short = -1

    val isSolid get() = color0.a() == 255
    val isFluid get() = color0.a() in 1 until 255
    val isWalkable get() = !isSolid && !isFluid
    val isGrassy = "grass" in typeUUID

    val color = getBlockColor(texId)

    var friction = 5f

    var droppedType: ItemType = this
    var droppedXpOrbs = 0

    override fun toString(): String {
        return nameDesc.name
    }

    fun getBounds(x: Int, y: Int, z: Int, dst: AABBd): AABBd {
        val cx = x.toDouble()
        val cy = y.toDouble()
        val cz = z.toDouble()
        val size = (this as? CustomBlockBounds)?.customSize ?: defaultBlockSize
        return dst.set(size).translate(cx, cy, cz)
    }

    fun toItem(metadata: Metadata?) = ItemSlot(this, 1, metadata)

    fun startFalling(x: Int, y: Int, z: Int, metadata: Metadata?): Boolean {
        val ownChunk = dimension.getChunkAt(x, y, z) ?: return false
        ownChunk.setBlock(x, y, z, BlockRegistry.Air)
        ownChunk.afterBlockChangeI(x, y, z)
        playStartFallingSound(getDropPosition(x, y, z), this)
        spawnEntity(MovingBlock(toItem(metadata)), getDropPosition(x, y, z))
        return true
    }

    fun dropAsItem(blockId: Vector3i, metadata: Metadata?, inHand: ItemSlot?): Boolean {
        return dropAsItem(blockId.x, blockId.y, blockId.z, metadata, inHand)
    }

    fun dropAsItem(x: Int, y: Int, z: Int, metadata: Metadata?, inHand: ItemSlot?): Boolean {
        val ownChunk = dimension.getChunkAt(x, y, z) ?: return false
        ownChunk.setBlock(x, y, z, BlockRegistry.Air)
        ownChunk.afterBlockChangeI(x, y, z)
        playBreakBlockSound(getDropPosition(x, y, z), this)
        // todo if tool has silktouch, drop this and no xp
        dropItem(x, y, z, toItem(metadata))
        repeat(droppedXpOrbs) { dropXpOrb(x, y, z) }
        return true
    }

}