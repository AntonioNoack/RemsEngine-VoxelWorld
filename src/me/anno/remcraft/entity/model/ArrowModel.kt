package me.anno.remcraft.entity.model

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.remcraft.entity.PlayerEntity

object ArrowModel : Model<PlayerEntity>() {
    private val mesh = PlaneCreator.createPlane(0f, 0f, 1f, 1f)
    override fun fill(transform: Transform, callback: (IMesh, Transform) -> Unit) {
        val velocity = physics.actualVelocity
        if (velocity.lengthSquared() > 0.01f) {
            transform.localRotation =
                transform.localRotation.rotationTo(
                    1f, 1f, 0f,
                    velocity.x, velocity.y, velocity.z,
                )
        }

        callback(mesh, transform)
    }
}