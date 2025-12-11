package me.anno.minecraft.entity

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.flatCube
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.ui.ItemSlot
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.floor

class MovingBlock(val stack: ItemSlot) : MovingEntity(size) {
    companion object {
        private val size = Vector3f(1f)

        fun playSetBlockSound(pos: Vector3d) {
            // todo play set block sound
        }

        fun playDropItemSound(pos: Vector3d) {
            // todo play drop item sound
        }

        private val mesh = flatCube.scaled(0.5f).front
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        // todo bake visuals into mesh instead of just a cube
        pipeline.addMesh(mesh, this, transform)
    }

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
            dst.setBlock(gx, gy, gz, stackType, stack.metadata)
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
            removeEntity()
        }
    }
}