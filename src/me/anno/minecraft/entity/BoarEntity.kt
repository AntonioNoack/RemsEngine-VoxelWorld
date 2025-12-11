package me.anno.minecraft.entity

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.PIf
import me.anno.minecraft.entity.TexturedCube.createCuboid
import me.anno.utils.OS.res
import org.joml.Vector3f

class BoarEntity : Animal(size) {
    companion object {
        private val size = Vector3f(0.9f)

        private val texture = res.getChild("textures/animals/Boar.png")

        val headMesh = createCuboid(
            19, 18, 15,
            0, 0,
            128, 64,
            texture
        )

        val bodyMesh = createCuboid(
            16, 32, 16,
            55, 15,
            128, 64,
            texture
        )

    }

    override fun fill(pipeline: Pipeline, transform: Transform) {

        val body = getTransform(0).place(0f, 0f, 0f, PIf * 0.5f, 0f, 0f, null)
        val head = getTransform(1).place(0f, 10f, 33f, 0f, 0f, 0f, body)

        pipeline.addMesh(bodyMesh, this, body)
        pipeline.addMesh(headMesh, this, head)

    }
}