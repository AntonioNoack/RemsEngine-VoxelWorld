package me.anno.minecraft.entity.model

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.posMod
import me.anno.minecraft.entity.Entity
import me.anno.minecraft.entity.MovingEntity
import kotlin.math.sin

abstract class Model<Self : Entity> {

    lateinit var self: Self

    val physics get() = (self as MovingEntity).physics
    fun getTransform(index: Int) = self.getTransform(index)

    fun getWalkingSwing(speed: Float): Float {
        val time = posMod(Time.gameTime, TAU).toFloat()
        val velocity = physics.actualVelocity.lengthXZ()
        val amplitude = velocity / (1f + velocity)
        return sin(speed * time) * 0.5f * amplitude
    }

    abstract fun fill(pipeline: Pipeline, transform: Transform)

}