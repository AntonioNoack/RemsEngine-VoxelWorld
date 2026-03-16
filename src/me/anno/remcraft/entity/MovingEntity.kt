package me.anno.remcraft.entity

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.remcraft.entity.physics.AABBPhysics
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.ui.ItemSlot
import me.anno.remcraft.ui.controls.GameMode
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

abstract class MovingEntity(halfExtents: Vector3f, texture: Texture) :
    RemcraftEntity(halfExtents, texture) {

    companion object {

        private const val VOXEL = 1f / 16f

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
                .mul(VOXEL)

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

    val physics = AABBPhysics(Vector3d(), halfExtents)

    var ridingOnEntity: RideableEntity? = null
    var gravityFactor = 1f

    open fun stepPhysics(dt: Float) {
        if (ridingOnEntity != null) {
            physics.acceleration.set(0f) // reset forces for next frame
            return
        }

        collectForces()
        if (this is PlayerEntity && gameMode == GameMode.SPECTATOR) {
            physics.stepSpectator(dt)
        } else {
            physics.step(dt)
            physics.applyFriction(dt)
        }
        physics.acceleration.set(0f) // reset forces for next frame
        physicsToEngine()
    }

    fun applyRiding() {
        if (ridingOnEntity != null) return // riding ourselves

        var base = this as? RideableEntity ?: return
        while (true) {
            val child = base.rider ?: break
            child.applyRidingTransform(base)
            base = child as? RideableEntity ?: break
        }
    }

    fun applyRidingTransform(base: RideableEntity) {
        val tr = transform ?: return
        val baseTr = (base as RemcraftEntity).transform ?: return

        tr.localPosition = baseTr.localPosition + baseTr.localUp() * base.ridingHeight.toDouble()
        tr.localRotation = baseTr.localRotation
    }

    fun Transform.localUp(): Vector3d {
        return localRotation.transform(Vector3d(0.0, 1.0, 0.0))
    }

    fun physicsToEngine() {
        val transform = transform ?: return
        transform.localPosition = physics.position
    }

    open fun collectForces() {
        val acceleration = physics.acceleration
        acceleration.fma(gravityFactor, dimension.gravity)
    }

    fun jump() {
        val dt = Time.deltaTime.toFloat()
        physics.acceleration.y += 7f / dt // dt = 1/60 -> 360?
        physics.actualVelocity.y += 0.001f // mark as already-jumped
    }

    open fun addItemFrom(stack: ItemSlot): Boolean = false
}