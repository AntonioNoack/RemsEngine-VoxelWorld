package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.minecraft.entity.PlayerEntity

object ArrowModel : Model<PlayerEntity>() {
    private val mesh = PlaneCreator.createPlane(0f, 0f, 1f, 1f)
    override fun fill(pipeline: Pipeline, transform: Transform) {
        val velocity = physics.actualVelocity
        if (velocity.lengthSquared() > 0.01f) {
            transform.localRotation =
                transform.localRotation.rotationTo(
                    1f, 1f, 0f,
                    velocity.x, velocity.y, velocity.z,
                )
        }

        pipeline.addMesh(mesh, self, transform)
    }
}