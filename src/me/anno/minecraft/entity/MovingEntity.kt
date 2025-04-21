package me.anno.minecraft.entity

import me.anno.Time
import me.anno.ecs.systems.OnUpdate
import me.anno.minecraft.rendering.v2.dimension
import org.joml.Vector3d
import org.joml.Vector3f

abstract class MovingEntity(size: Vector3f) : Entity(), OnUpdate {

    val physics = AABBPhysics(Vector3d(), size)
    val moveIntend = Vector3f()

    override fun onUpdate() {
        stepPhysics(Time.deltaTime.toFloat())
    }

    open fun stepPhysics(dt: Float) {
        collectForces()
        physics.step(dimension, dt)
        physics.applyFriction(dt)
        physicsToEngine()
    }

    fun physicsToEngine() {
        val transform = transform ?: return
        transform.localPosition = physics.position
    }

    open fun collectForces() {
        val acceleration =physics.acceleration
        acceleration.set(dimension.gravity)
        acceleration.add(moveIntend)
    }

}