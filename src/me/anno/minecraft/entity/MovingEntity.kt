package me.anno.minecraft.entity

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.systems.OnUpdate
import me.anno.minecraft.rendering.v2.dimension
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

abstract class MovingEntity(size: Vector3f) : Entity(size), OnUpdate {

    companion object {
        fun Transform.place(
            bx: Float, by: Float, bz: Float,
            dx: Float, dy: Float, dz: Float,
            rx: Float, ry: Float, rz: Float,
            parent: Transform?
        ): Transform {

            val q = Quaternionf()
                .rotationYXZ(ry, rx, rz)

            val v = Vector3f(-dx, -dy, -dz)
                .rotate(q)
                .add(bx + dx, by + dy, bz + dz)

            if (parent != null) {
                parent.validate()
                val par = parent.getLocalTransform(Matrix4x3())
                par.transformPosition(v)
                val tmp = par.getUnnormalizedRotation(Quaternionf())
                tmp.mul(q, q)
            }

            setLocalPosition(v)
            setLocalRotation(q)
            return this
        }

        fun Transform.place(
            bx: Float, by: Float, bz: Float,
            rx: Float, ry: Float, rz: Float,
            parent: Transform?
        ) = place(bx, by, bz, 0f, 0f, 0f, rx, ry, rz, parent)
    }

    val physics = AABBPhysics(Vector3d(), size)
    val moveIntend = Vector3f()
    var gravityFactor = 1f

    override fun onUpdate() {
        stepPhysics(Time.deltaTime.toFloat())
        invalidateBounds()
    }

    open fun stepPhysics(dt: Float) {
        collectForces()
        if (this is PlayerEntity && spectatorMode) {
            physics.stepSpectator(dt)
        } else {
            physics.step(dimension, dt)
            physics.applyFriction(dt)
        }
        physicsToEngine()
    }

    fun physicsToEngine() {
        val transform = transform ?: return
        transform.localPosition = physics.position
    }

    open fun collectForces() {
        val acceleration = physics.acceleration
        dimension.gravity.mul(gravityFactor, acceleration)
        acceleration.add(moveIntend)
    }

}