package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.gpu.GFX
import me.anno.input.Input
import org.lwjgl.glfw.GLFW

class PlayerController : Component(), ControlReceiver {

    // todo create camera
    // todo create player?
    // todo control camera & player
    // todo bullet physics or physics like DigitalCampus

    var camera: CameraComponent? = null
    var player: Player? = null

    var controlX = 0f
    var controlZ = 0f

    override fun onUpdate(): Int {

        if (player == null) player = entity?.getComponent(Player::class)
        if (camera == null) camera = entity?.getComponent(CameraComponent::class)

        controlX = 0f
        controlZ = 0f

        if (Input.isKeyDown('w')) controlZ++
        if (Input.isKeyDown('s')) controlZ--
        if (Input.isKeyDown('a')) controlX--
        if (Input.isKeyDown('d')) controlX++

        val transform = camera?.transform
        if (transform != null) {
            transform.localRotation =
                transform.localRotation
                    .identity()
                    .rotateX(camX.toDouble())
                    .rotateY(camY.toDouble())
        }

        return 1
    }

    fun canJump(): Boolean {
        // todo check whether player is on floor and not in water
        return true
    }

    fun jump() {

    }

    fun tryToJump() {
        if (canJump()) jump()
    }

    override fun onKeyTyped(key: Int): Boolean {
        return if (key == GLFW.GLFW_KEY_SPACE) {
            tryToJump()
            true
        } else false
    }

    var camX = 0f
    var camY = 0f

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        // turn the camera
        val speed = 3f / GFX.height
        camX += dy * speed
        camY += dx * speed
        return true
    }

    override fun clone(): PlayerController {
        val clone = PlayerController()
        copy(clone)
        return clone
    }

}