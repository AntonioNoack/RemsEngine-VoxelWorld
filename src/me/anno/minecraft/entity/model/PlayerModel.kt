package me.anno.minecraft.entity.model

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.posMod
import me.anno.minecraft.entity.MovingEntity.Companion.place
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.entity.model.CuboidCreator.createCuboidX2

class PlayerModel(val male: Boolean) : Model<PlayerEntity>() {

    private val texSize = getSize(64, 64)

    private val headMesh = createCuboidX2(
        8, 8, 8,
        0, 0, 32, 0,
        texSize
    )

    private val torsoMesh = createCuboidX2(
        8, 12, 4,
        16, 16, 16, 32,
        texSize
    )

    // left from front, right from self
    private val rightArmMesh = createCuboidX2(
        3, 12, 4,
        40, 16, 40, 32,
        texSize
    )

    private val leftArmMesh = createCuboidX2(
        3, 12, 4,
        32, 48, 46, 48,
        texSize
    )

    // left from front, right from self
    private val rightLegMesh = createCuboidX2(
        4, 12, 4,
        0, 16, 0, 32,
        texSize
    )

    private val leftLegMesh = createCuboidX2(
        4, 12, 4,
        16, 48, 0, 48,
        texSize
    )

    override fun fill(pipeline: Pipeline, transform: Transform) {
        // todo swim/fly
        // todo crouch
        // todo mine/break/touch
        // todo open chest
        // todo jump?

        val dt = Time.deltaTime.toFloat()
        val swing = getWalkingSwing(6f)

        val angle0 = physics.actualVelocity.angleY() - self.bodyRotationY

        val torso = getTransform(0).place(0f, 2f, 0f, 0f, angle0, 0f, null)
        pipeline.addMesh(torsoMesh, self, torso)

        val dr = PIf * 0.5f
        val thy = posMod(-angle0 - dr, PIf) - dr
        val sign = if (posMod(thy + angle0 + dr, TAUf) > PIf) -1f else +1f
        self.targetHeadY += (thy - self.targetHeadY) * dtTo01(dt * 3f)
        val head = getTransform(1).place(
            0f, 10f, 0f,
            sign * self.headRotationX, self.targetHeadY,
            0f, torso
        )
        pipeline.addMesh(headMesh, self, head)

        val dy = 6f

        val rightLeg = getTransform(2).place(
            -2f, -12f, 0f,
            0f, dy, 0f, -swing, 0f, 0f,
            torso
        )
        val leftLeg = getTransform(3).place(
            +2f, -12f, 0f,
            0f, dy, 0f, +swing, 0f, 0f,
            torso
        )
        pipeline.addMesh(rightLegMesh, self, rightLeg)
        pipeline.addMesh(leftLegMesh, self, leftLeg)

        val armOffset = if (male) 6f else 5f
        val rightArm = getTransform(4).place(
            -armOffset, 0f, 0f,
            0f, dy, 0f, -swing, 0f, 0f,
            torso
        )
        val leftArm = getTransform(5).place(
            +armOffset, 0f, 0f,
            0f, dy, 0f, +swing, 0f, 0f,
            torso
        )
        pipeline.addMesh(rightArmMesh, self, rightArm)
        pipeline.addMesh(leftArmMesh, self, leftArm)
    }

}