package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.minecraft.multiplayer.NetworkData
import me.anno.minecraft.ui.ControlMode
import org.joml.Vector3d
import org.joml.Vector3f

class Player(var isPrimary: Boolean, name: String) : MovingEntity(playerSize) {

    constructor() : this(false, "Gustav${(Math.random() * 1e6).toInt()}")

    companion object {
        private val playerSize = Vector3f(0.6f, 1.8f, 0.6f)
        private fun Vector3f.mulAdd(fa: Float, b: Vector3d, dst: Vector3d) {
            dst.set(b.x + x * fa, b.y + y * fa, b.z + z * fa)
        }
    }

    init {
        this.name = name
    }

    val networkData = NetworkData()
    var playMode = ControlMode.SURVIVAL
    var shallFly = false
    var bodyRotationY = 0f
    var headRotationX = 0f

    override fun collectForces() {
        if (playMode == ControlMode.CREATIVE && shallFly) {
            physics.acceleration.set(moveIntend)
        } else super.collectForces()
    }

    override fun stepPhysics(dt: Float) {
        if (playMode == ControlMode.SPECTATOR) {
            moveIntend.mulAdd(dt, physics.velocity, physics.velocity)
            physics.velocity.mulAdd(dt, physics.position, physics.position)
            physics.applyFriction(dt)
            physicsToEngine()
        } else super.stepPhysics(dt)
    }

    override val className: String = "MCPlayer"

    override fun clone(): Component {
        val clone = Player(isPrimary, name)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Player
        dst.isPrimary = isPrimary
    }

}