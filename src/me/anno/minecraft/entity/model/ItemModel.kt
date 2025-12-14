package me.anno.minecraft.entity.model

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.posMod
import me.anno.mesh.Shapes.flatCube
import me.anno.minecraft.entity.ItemEntity
import kotlin.math.sin

object ItemModel : Model<ItemEntity>() {

    private const val Y_SWINGING = 0.3f
    private val mesh = flatCube.scaled(0.2f).front

    private val angle: Float
        get() = posMod(Time.gameTime - self.spawnTime, TAU).toFloat()

    override fun fill(pipeline: Pipeline, transform: Transform) {
        val tr = getTransform(0)
        val angle = angle
        val y = sin(angle * 2f) * Y_SWINGING
        tr.localPosition = tr.localPosition.set(0f, y, 0f)
        tr.localRotation = tr.localRotation.rotationY(angle)
        pipeline.addMesh(mesh, self, transform)
    }
}