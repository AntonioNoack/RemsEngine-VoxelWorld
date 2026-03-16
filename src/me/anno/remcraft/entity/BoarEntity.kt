package me.anno.remcraft.entity

import me.anno.remcraft.entity.ai.FindTargets.findGrassyBlock
import me.anno.remcraft.entity.model.Model
import me.anno.remcraft.entity.model.PigModel
import me.anno.utils.OS.res
import org.joml.Vector3f
import org.joml.Vector3i

class BoarEntity : Animal(halfExtents, texture), RideableEntity {

    companion object {
        private val halfExtents = Vector3f(7f / 16f)
        private val texture = Texture(res.getChild("textures/animals/Boar.png"))
    }

    override val model: Model<*> get() = PigModel
    override val maxJumpDown: Int get() = 2
    override val maxHealth: Int get() = 8
    override val ridingHeight: Float get() = 0.5f
    override var rider: MovingEntity? = null

    override fun findTarget(start: Vector3i, seed: Long): Vector3i? {
        return findGrassyBlock(start, 16.0, 16, 1, true, seed)
    }
}