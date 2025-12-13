package me.anno.minecraft.entity

import me.anno.Time
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mixAngle
import me.anno.maths.Maths.sq
import me.anno.minecraft.entity.ai.FindTargets
import me.anno.minecraft.entity.ai.PathFinding
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PigModel
import me.anno.ui.UIColors
import me.anno.utils.OS.res
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

class BoarEntity : Animal(halfExtents) {

    companion object {
        private val halfExtents = Vector3f(7f / 16f)
        private val boarModel = PigModel(res.getChild("textures/animals/Boar.png"))
    }

    override val model: Model<*>
        get() = boarModel

    var thinkTimeout = 0f
    val pathFinding = PathFinding(halfExtents)

    override fun onUpdate() {
        super.onUpdate()
        executeAI()
        pathFinding.debugDraw()
    }

    fun executeAI() {
        // thinking
        thinkTimeout -= Time.deltaTime.toFloat()
        if (thinkTimeout < 0f) {
            tryToWalkToTarget()
        }
    }

    fun tryToWalkToTarget() {
        val target = pathFinding.nextTarget(position)
        if (target >= 0) walkTowardsTarget(target)
        else findNextTarget()
    }

    fun findNextTarget() {
        // find next target and path to it
        val seed = Maths.randomLong()
        val start = FindTargets.getPosition(this)
        val target = FindTargets.findGrassyBlock(start, 16.0, 16, 1, true, seed)
        if (target != null) {
            val foundPath = pathFinding.findPathTo(start, target, 1, 2)
            if (!foundPath) thinkTimeout = 2f
        } else {
            // wait a little
            thinkTimeout = 1f
        }
    }

    fun walkTowardsTarget(target: Int) {
        val pos = position
        val dirX = (pathFinding.getX(target) - pos.x).toFloat()
        val dirY = (pathFinding.getY(target) - pos.y).toFloat()
        val dirZ = (pathFinding.getZ(target) - pos.z).toFloat()

        DebugShapes.debugArrows.add(
            DebugLine(
                pos, Vector3d(pos).add(dirX, dirY, dirZ),
                UIColors.midOrange, 0f
            )
        )

        val distance = length(dirX, dirZ)
        val scale = 20f / (1f + distance)

        // rotate towards target
        if (distance > 0.1f) {
            val transform = transform ?: return
            val dt = Time.deltaTime.toFloat()
            val targetAngle = atan2(dirX, dirZ)
            transform.localRotation = transform.localRotation
                .rotationY(mixAngle(bodyRotationY, targetAngle, dtTo01(dt * 3f)))
        }

        // todo accelerate towards target velocity?
        // if needs to jump, give extra acceleration
        val shallJump = dirY > 0.3f && max(abs(dirX), abs(dirZ)) < 1.1f * (halfExtents.x + 0.5f)
        physics.acceleration.add(dirX * scale, 0f, dirZ * scale)
        if (shallJump && physics.isOnGround) jump()

    }
}