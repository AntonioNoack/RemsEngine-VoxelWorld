package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.pipeline.Pipeline
import me.anno.minecraft.entity.PlayerEntity

object ArrowModel : Model<PlayerEntity>() {
    private val mesh = Mesh().apply {
        val halfSize = 0.5f
        positions = floatArrayOf(
            -halfSize, -halfSize, 0f,
            -halfSize, +halfSize, 0f,
            +halfSize, -halfSize, 0f,
            +halfSize, +halfSize, 0f,
        )
        uvs = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )
        normals = floatArrayOf(
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
        )
        // both-sided
        indices = intArrayOf(
            0, 1, 3, 0, 3, 1,
            0, 3, 2, 0, 2, 3,
        )
    }

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