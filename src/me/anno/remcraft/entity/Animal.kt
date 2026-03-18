package me.anno.remcraft.entity

import me.anno.Time
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.remcraft.entity.ai.AnimalAI.executeAI
import me.anno.remcraft.entity.ai.PathFinding
import me.anno.remcraft.entity.effect.StatusEffect
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3i

abstract class Animal(halfExtents: Vector3f, texture: Texture) : MovingEntity(halfExtents, texture), OnUpdate {

    val effects = ArrayList<StatusEffect>()

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
        executeAI()
        pathFinding.debugDraw()
    }

    fun processEffects() {
        val dt = Time.deltaTime.toFloat()
        for (i in effects.indices) {
            val effect = effects[i]
            processEffect(effect)
            effect.remainingDuration -= dt
        }
        effects.removeIf { it.remainingDuration <= 0f }
    }

    fun processEffect(effect: StatusEffect) {
        // todo implement this
    }

    var health = maxHealth.toFloat()

    var thinkTimeout = 0f
    val pathFinding = PathFinding(halfExtents)

    abstract fun findTarget(start: Vector3i, seed: Long): Vector3i?

    fun damage(deltaHealth: Float) {
        health -= deltaHealth
        if (health <= 0f) {
            health = 0f
            onDeath()
        }
    }

    open fun onDeath() {
        // todo if is player, respawn somehow & show death screen
        // todo drop loot
        removeFromWorld()
    }

    abstract val maxJumpDown: Int
    abstract val maxHealth: Int

}