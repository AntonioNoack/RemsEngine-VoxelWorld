package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.files.FileReference
import me.anno.minecraft.entity.MovingEntity.Companion.place
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.entity.Texture
import me.anno.minecraft.entity.model.CuboidCreator.createCuboid

class PigModel(src: FileReference) : Model<PlayerEntity>() {

    private val texture = Texture(src, 64, 32)

    val headMesh = createCuboid(
        8, 8, 8,
        0, 0,
        texture
    )

    val snotMesh = createCuboid(
        4, 3, 1,
        16, 16,
        texture
    )

    val bodyMesh = createCuboid(
        10, 16, 8,
        28, 8,
        texture
    ).apply { rotateX90Degrees() }

    val legMesh = createCuboid(
        4, 6, 4,
        0, 16,
        texture
    )

    override fun fill(pipeline: Pipeline, transform: Transform) {

        val swing = getWalkingSwing(12f)

        val body = getTransform(0).place(0f, 3f, 0f, 0f, 0f, 0f, null)
        val head = getTransform(1).place(0f, 4f, 10f, 0f, 0f, 0f, body)
        val snot = getTransform(2).place(0f, -1.5f, 4.5f, 0f, 0f, 0f, head)

        val dx = 2.9f
        val leftLeg = getTransform(3).place(+dx, -7f, -7f, 0f, 3f, 0f, +swing, 0f, 0f, body)
        val rightLeg = getTransform(4).place(-dx, -7f, -7f, 0f, 3f, 0f, -swing, 0f, 0f, body)
        val leftArm = getTransform(5).place(+dx, -7f, +5f, 0f, 3f, 0f, -swing, 0f, 0f, body)
        val rightArm = getTransform(6).place(-dx, -7f, +5f, 0f, 3f, 0f, +swing, 0f, 0f, body)

        pipeline.addMesh(bodyMesh, self, body)
        pipeline.addMesh(headMesh, self, head)
        pipeline.addMesh(snotMesh, self, snot)
        pipeline.addMesh(legMesh, self, leftLeg)
        pipeline.addMesh(legMesh, self, rightLeg)
        pipeline.addMesh(legMesh, self, leftArm)
        pipeline.addMesh(legMesh, self, rightArm)
    }

}