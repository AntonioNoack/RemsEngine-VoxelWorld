package me.anno.minecraft.entity

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.posMod
import me.anno.minecraft.entity.TexturedCube.createCuboidX2
import me.anno.minecraft.multiplayer.NetworkData
import me.anno.utils.OS.res
import org.joml.Vector3f
import kotlin.math.sin

class PlayerEntity(var isPrimary: Boolean, name: String) : Animal(playerSize) {

    constructor() : this(false, "Gustav${(Math.random() * 1e6).toInt()}")

    companion object {
        private val playerSize = Vector3f(0.6f, 1.8f, 0.6f)

        private val texture = res.getChild("textures/players/Reyviee.png")

        val headMesh = createCuboidX2(
            8, 8, 8,
            0, 0, 32, 0,
            64, 64,
            texture
        )

        val torsoMesh = createCuboidX2(
            8, 12, 4,
            16, 16, 16, 32,
            64, 64,
            texture
        )

        // left from front, right from self
        val rightArmMesh = createCuboidX2(
            3, 12, 4,
            40, 16, 40, 32,
            64, 64,
            texture
        )

        val leftArmMesh = createCuboidX2(
            3, 12, 4,
            32, 48, 46, 48,
            64, 64,
            texture
        )

        // left from front, right from self
        val rightLegMesh = createCuboidX2(
            4, 12, 4,
            0, 16, 0, 32,
            64, 64,
            texture
        )

        val leftLegMesh = createCuboidX2(
            4, 12, 4,
            16, 48, 0, 48,
            64, 64,
            texture
        )

        private const val VOXEL = 1f / 16f

    }

    init {
        this.name = name
    }

    val networkData = NetworkData()
    var spectatorMode = false
    val bodyRotationY get() = bodyRotation.getEulerAngleYXZvY()
    val headRotationX get() = headRotation.getEulerAngleYXZvX()

    var targetHeadY = 0f
    override fun fill(pipeline: Pipeline, transform: Transform) {
        transform.validate()

        // todo swim/fly
        // todo crouch
        // todo mine/break/touch
        // todo open chest
        // todo jump?

        val dt = Time.deltaTime.toFloat()
        val time = posMod(Time.gameTime, TAU).toFloat()
        val velocity = physics.actualVelocity.lengthXZ()
        val amplitude = velocity / (1f + velocity)
        val swing = sin(6f * time) * 0.5f * amplitude

        val angle0 = physics.actualVelocity.angleY() - bodyRotationY

        val torso = getTransform(0).place(0f, 2f * VOXEL, 0f, 0f, angle0, 0f, null)
        pipeline.addMesh(torsoMesh, this, torso)

        val dr = PIf * 0.5f
        val thy = posMod(-angle0 - dr, PIf) - dr
        val sign = if (posMod(thy + angle0 + dr, TAUf) > PIf) -1f else +1f
        targetHeadY += (thy - targetHeadY) * dtTo01(dt * 3f)
        val head = getTransform(1).place(
            0f, 10f * VOXEL, 0f,
            sign * headRotationX, targetHeadY,
            0f, torso
        )
        pipeline.addMesh(headMesh, this, head)

        val dy = 6f * VOXEL

        val rightLeg = getTransform(2).place(
            -2f * VOXEL, -12f * VOXEL, 0f,
            0f, dy, 0f, -swing, 0f, 0f,
            torso
        )
        val leftLeg = getTransform(3).place(
            +2f * VOXEL, -12f * VOXEL, 0f,
            0f, dy, 0f, +swing, 0f, 0f,
            torso
        )
        pipeline.addMesh(rightLegMesh, this, rightLeg)
        pipeline.addMesh(leftLegMesh, this, leftLeg)

        val male = false
        val armOffset = if (male) 6f else 5f
        val rightArm = getTransform(4).place(
            -armOffset * VOXEL, 0f, 0f,
            0f, dy, 0f, -swing, 0f, 0f,
            torso
        )
        val leftArm = getTransform(5).place(
            +armOffset * VOXEL, 0f, 0f,
            0f, dy, 0f, +swing, 0f, 0f,
            torso
        )
        pipeline.addMesh(rightArmMesh, this, rightArm)
        pipeline.addMesh(leftArmMesh, this, leftArm)
    }

    override val className: String = "MCPlayer"

    override fun clone(): Component {
        val clone = PlayerEntity(isPrimary, name)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PlayerEntity
        dst.isPrimary = isPrimary
    }

}