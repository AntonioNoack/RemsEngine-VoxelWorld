package me.anno.minecraft.entity

import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.MovingBlockModel
import me.anno.minecraft.item.ItemType
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.ui.ItemSlot
import me.anno.utils.OS.res
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.floor

class MovingBlock(val stack: ItemSlot) : MovingEntity(halfExtents, texture) {

    companion object {
        // 0.45 instead of 0.50 is necessary to avoid getting stuck on edges
        private val halfExtents = Vector3f(0.45f)

        fun playSetBlockSound(pos: Vector3d) {
            // todo play set block sound
        }

        fun playDropItemSound(pos: Vector3d) {
            // todo play drop item sound
        }

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
        super.onUpdate()
        if (physics.isOnGround) {
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
        var count = stack.count
        if (here == BlockRegistry.Air && dst != null && stackType is BlockType) {
            // set block -> destroy entity
            playSetBlockSound(soundPos)
            dimension.setBlockAt(gx, gy, gz, dst, stackType, stack.metadata)
            dimension.invalidateAt(gx, gy, gz, stackType)
            count--
        }
        if (count > 0) {
            // drop item -> replace component
            playDropItemSound(soundPos)
            val entity = entity ?: return
            removeFromParent()
            val newStack =
                if (count == stack.count) stack
                else ItemSlot(stack.type, count, stack.metadata)
            entity.add(ItemEntity(newStack))
        } else {
            // finished :)
            destroyEntity()
        }
    }
}