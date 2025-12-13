package me.anno.minecraft.entity

import me.anno.minecraft.entity.ai.FindTargets.findGrassyBlock
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PigModel
import me.anno.utils.OS.res
import org.joml.Vector3f
import org.joml.Vector3i

class BoarEntity : Animal(halfExtents) {

    companion object {
        private val halfExtents = Vector3f(7f / 16f)
        private val boarModel = PigModel(res.getChild("textures/animals/Boar.png"))
    }

    override val model: Model<*>
        get() = boarModel

    override val maxJumpDown: Int
        get() = 2

    override fun findTarget(start: Vector3i, seed: Long): Vector3i? {
        return findGrassyBlock(start, 16.0, 16, 1, true, seed)
    }
}