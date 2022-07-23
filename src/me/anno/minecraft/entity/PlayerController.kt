package me.anno.minecraft.entity

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3f
import kotlin.math.sqrt

class PlayerController : Component(), ControlReceiver {

    // todo create camera
    // todo create player?
    // todo control camera & player
    // todo bullet physics or physics like DigitalCampus

    var camera: Camera? = null
    var player: Player? = null

    val inputCollector = Vector3f()
    val acceleration = Vector3f()
    val velocity = Vector3f()

    val headRotation = Vector3f()

    var isCreative = true
    var gravity = Vector3f(0f, -9.81f, 0f)

    var maximumVelocity = 10f

    // 0 = ice, 1 = hard rough rock
    @Range(0.0, 1.0)
    var friction = 0.9f

    override fun onKeyDown(key: Int): Boolean {
        println("key $key went down")
        return true
    }

    override fun onUpdate(): Int {

        val entity = entity
        if (entity != null) {
            if (player == null) player = entity.getComponent(Player::class)
            if (camera == null) camera = entity.getComponent(Camera::class)
        }

        val dt = Engine.deltaTime
        val dtx = clamp(10f * friction * dt)

        val inputCollector = inputCollector
        val acceleration = acceleration
        val velocity = velocity

        val grip = maximumVelocity
        if (Input.isKeyDown('w')) inputCollector.z += grip
        if (Input.isKeyDown('s')) inputCollector.z -= grip
        if (Input.isKeyDown('a')) inputCollector.x -= grip
        if (Input.isKeyDown('d')) inputCollector.x += grip

        // normalize input to |1|
        val ils = inputCollector.lengthSquared()
        if (ils > grip * grip) {
            inputCollector.div(sqrt(grip * grip / ils))
        }

        if (isCreative) {
            if (Input.isKeyDown('q')) inputCollector.y++
            if (Input.isKeyDown('e')) inputCollector.y--
        } else {
            inputCollector.add(gravity)
        }

        inputCollector.rotateY(headRotation.y)
        acceleration.set(inputCollector)
        inputCollector.set(0f)

        velocity.mul(1f - dtx)
        acceleration.mulAdd(dtx, velocity, velocity)

        val selfT = transform
        if (selfT != null) {
            val dt2 = dtx.toDouble()
            selfT.localPosition.add(velocity.x * dt2, velocity.y * dt2, velocity.z * dt2)
            selfT.invalidateLocal()
        }

        val camT = camera?.transform
        if (camT != null) {
            val tmp = JomlPools.vec3d.borrow()
            camT.setGlobalRotation(tmp.set(headRotation))
        }

        return 1
    }

    fun canJump(): Boolean {
        // todo check whether player is on floor and not in water
        return true
    }

    fun jump() {
        velocity.add(0f, maximumVelocity, 0f)
    }

    fun tryToJump() {
        if (canJump()) jump()
    }

    override fun onKeyTyped(key: Int): Boolean {
        return if (key == ' '.code) {
            tryToJump()
            true
        } else false
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        // turn the camera
        val window = GFX.someWindow
        val speed = 3f / window.height
        val headRotation = headRotation
        headRotation.x += dy * speed
        headRotation.y += dx * speed
        return true
    }

    override fun clone(): PlayerController {
        val clone = PlayerController()
        copy(clone)
        return clone
    }

    override val className: String = "PlayerController"

}