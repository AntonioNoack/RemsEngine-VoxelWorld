package me.anno.minecraft.entity

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.posMod
import me.anno.mesh.Shapes.flatCube
import me.anno.minecraft.ui.ItemSlot
import org.joml.Vector3f
import kotlin.math.sin

class ItemEntity(val stack: ItemSlot) : MovingEntity(size) {

    companion object {
        private const val Y_SWINGING = 0.3f

        private val size = Vector3f(0.4f)
        private val mesh = flatCube.scaled(size.x * 0.33f).front
    }

    var spawnTime = Time.gameTime

    val angle: Float
        get() = posMod(Time.gameTime - spawnTime, TAU).toFloat()

    override fun fill(pipeline: Pipeline, transform: Transform) {
        // todo bake visuals into mesh instead of just a cube
        val tr = getTransform(0)
        val angle = angle
        val y = sin(angle * 2f) * Y_SWINGING
        tr.localPosition = tr.localPosition.set(0f, y, 0f)
        tr.localRotation = tr.localRotation.rotationY(angle)
        pipeline.addMesh(mesh, this, transform)
    }
}