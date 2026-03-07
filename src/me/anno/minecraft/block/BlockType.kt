package me.anno.minecraft.block

import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.minecraft.audio.playBreakBlockSound
import me.anno.minecraft.audio.playStartFallingSound
import me.anno.minecraft.block.types.CustomBlockBounds
import me.anno.minecraft.entity.Entity.Companion.spawnEntity
import me.anno.minecraft.entity.ItemEntity
import me.anno.minecraft.entity.MovingBlock
import me.anno.minecraft.entity.XpOrbEntity
import me.anno.minecraft.item.ItemType
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.ui.ItemSlot
import me.anno.utils.Color.a
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3i

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

    fun getBounds(x: Int, y: Int, z: Int, dst: AABBd): AABBd {
        val cx = x.toDouble()
        val cy = y.toDouble()
        val cz = z.toDouble()
        val size = (this as? CustomBlockBounds)?.customSize ?: defaultBlockSize
        return dst.set(size).translate(cx, cy, cz)
    }

    fun toItem(metadata: Metadata?) = ItemSlot(this, 1, metadata)

    fun getDropPosition(x: Int, y: Int, z: Int): Vector3d {
        val center = 0.5
        return Vector3d(x + center, y + center, z + center)
    }

    fun startFalling(x: Int, y: Int, z: Int, metadata: Metadata?): Boolean {
        val ownChunk = dimension.getChunkAt(x, y, z) ?: return false
        ownChunk.setBlock(x, y, z, BlockRegistry.Air)
        ownChunk.afterBlockChangeI(x, y, z)
        playStartFallingSound(getDropPosition(x, y, z), this)
        spawnEntity(MovingBlock(toItem(metadata)), getDropPosition(x, y, z))
        return true
    }

    fun dropAsItem(blockId: Vector3i, metadata: Metadata?): Boolean {
        return dropAsItem(blockId.x, blockId.y, blockId.z, metadata)
    }

    fun dropAsItem(x: Int, y: Int, z: Int, metadata: Metadata?): Boolean {
        val ownChunk = dimension.getChunkAt(x, y, z) ?: return false
        ownChunk.setBlock(x, y, z, BlockRegistry.Air)
        ownChunk.afterBlockChangeI(x, y, z)
        playBreakBlockSound(getDropPosition(x, y, z), this)
        val speed = 2f
        spawnEntity(ItemEntity(toItem(metadata)), getDropPosition(x, y, z))
            .physics.setVelocity(
                (Maths.random().toFloat() - 0.5f) * speed,
                (Maths.random().toFloat() - 0.5f) * speed,
                (Maths.random().toFloat() - 0.5f) * speed
            )
        repeat(3) {
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
        return true
    }

}