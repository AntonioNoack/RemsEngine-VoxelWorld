package me.anno.remcraft.entity

import me.anno.ecs.systems.OnUpdate
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.remcraft.audio.playDropItemSound
import me.anno.remcraft.audio.playSetBlockSound
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.entity.model.Model
import me.anno.remcraft.entity.model.MovingBlockModel
import me.anno.remcraft.item.ItemType
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.ui.ItemSlot
import me.anno.utils.OS.res
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.floor

class MovingBlock(val stack: ItemSlot) : MovingEntity(halfExtents, texture), OnUpdate {

    companion object {
        // 0.45 instead of 0.50 is necessary to avoid getting stuck on edges
        private val halfExtents = Vector3f(0.45f)

        private val texture = Texture(res.getChild("textures/blocks/Blocks.png"))
        private val blockModel = LazyMap { type: ItemType ->
            val block = (type as? BlockType) ?: BlockRegistry.Unknown
            val texId = block.texId
            MovingBlockModel(
                16, 16, 16,
                texId.and(15) * 16, texId.shr(4) * 16,
                getSize(256, 512)
            )
        }
    }

    override val model: Model<*>
        get() = blockModel[stack.type]

    override fun onUpdate() {
        if (physics.isOnGround && stack.count > 0) {
            // remove this, set a block
            setHere(physics.position)
        }
    }

    fun setHere(position: Vector3d) {
        val gx = floor(position.x).toInt()
        val gy = floor(position.y).toInt()
        val gz = floor(position.z).toInt()
        val here = dimension.getBlockAt(gx, gy, gz)
        val dst = dimension.getChunkAt(gx, gy, gz, Int.MAX_VALUE)
        val center = 0.5
        val soundPos = Vector3d(gx + center, gy + center, gz + center)
        val stackType = stack.type
        if (here == BlockRegistry.Air && dst != null && stackType is BlockType) {
            // set block -> destroy entity
            playSetBlockSound(soundPos, stackType)
            dst.setBlock(gx, gy, gz, stackType, stack.metadata)
            dst.afterBlockChange(gx, gy, gz)
            stack.count--
        }
        if (stack.count > 0) {
            // drop item -> replace component
            playDropItemSound(soundPos, stackType)
            val entity = entity ?: return
            removeFromParent()
            entity.add(ItemEntity(stack))
        } else {
            // finished :)
            destroyEntity()
        }
    }
}