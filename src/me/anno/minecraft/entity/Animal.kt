package me.anno.minecraft.entity

import me.anno.Time
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mixAngle
import me.anno.minecraft.entity.ai.FindTargets
import me.anno.minecraft.entity.ai.PathFinding
import me.anno.ui.UIColors
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max

abstract class Animal(halfExtents: Vector3f, texture: Texture) : MovingEntity(halfExtents, texture) {

    val bodyRotation: Quaternionf get() = transform!!.localRotation
    val headRotation = Quaternionf()

    val bodyRotationY get() = bodyRotation.getEulerAngleYXZvY()
    val headRotationX get() = headRotation.getEulerAngleYXZvX()

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Animal
        dst.headRotation.set(headRotation)
    }

    override fun onUpdate() {
        super.onUpdate()
        executeAI()
        pathFinding.debugDraw()
    }

    var health = maxHealth

    var thinkTimeout = 0f
    val pathFinding = PathFinding(halfExtents)

    abstract fun findTarget(start: Vector3i, seed: Long): Vector3i?
    abstract val maxJumpDown: Int
    abstract val maxHealth: Int

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
        val start = FindTargets.getPosition(this)
        val target = findTarget(start, Maths.randomLong())
        if (target != null) {
            val height = ceil(halfExtents.y * 2f).toInt()
            val foundPath = pathFinding.findPathTo(start, target, height, maxJumpDown)
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