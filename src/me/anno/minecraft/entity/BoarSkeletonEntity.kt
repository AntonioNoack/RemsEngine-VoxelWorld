package me.anno.minecraft.entity

import me.anno.maths.Maths.sq
import me.anno.minecraft.entity.ai.FindTargets.findGrassyBlock
import me.anno.minecraft.entity.ai.FindTargets.findPlayerTarget
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PigModel
import me.anno.utils.OS.res
import org.joml.Vector3f
import org.joml.Vector3i

class BoarSkeletonEntity : Animal(halfExtents) {

    companion object {
        private val halfExtents = Vector3f(7f / 16f)
        private val boarModel = PigModel(res.getChild("textures/animals/BoarSkeleton.png"))
    }

    override val model: Model<*>
        get() = boarModel

    override val maxJumpDown: Int
        get() = 2

    // todo when player is no longer on path, calculate a new path
    // todo abstract this into general hostile AI

    var playerTarget: PlayerEntity? = null

    fun findPlayerTarget(): PlayerEntity? {
        val old = playerTarget
        return if (old != null && old.position.distanceSquared(position) < sq(32.0)) old
        else findPlayerTarget(this, 16.0)
    }

    override fun findTarget(start: Vector3i, seed: Long): Vector3i? {
        val playerTarget = findPlayerTarget()
        this.playerTarget = playerTarget
        return playerTarget?.blockPosition?.apply { y-- }
            ?: findGrassyBlock(start, 16.0, 16, 1, true, seed)
    }
}