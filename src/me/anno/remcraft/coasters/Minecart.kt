package me.anno.remcraft.coasters

import me.anno.io.files.InvalidRef
import me.anno.remcraft.entity.*
import me.anno.remcraft.entity.model.Model
import me.anno.remcraft.ui.ItemSlot
import me.anno.remcraft.ui.controls.RemcraftControls
import org.joml.Vector3f

// todo rideable
// todo slide on tracks until the end:
//  t = estimate progress from tracks
//  pos += sign(vel * dir(t)) * dir(t) * |vel| * dt
//  energy = vel*vel + prevPos.y * C
//  vel = sqrt(max(0, energy - pos.y * C))
// todo bump if stopped
class Minecart : MovingEntity(Vector3f(0.5f), Texture(InvalidRef)), RideableEntity, RightClickAnimal {

    var animPosition = 0f
    var steering = 0f

    override var rider: MovingEntity? = null

    // todo this needs rotational speed, rotational physics

    override val ridingHeight: Float get() = 0.3f
    override val model: Model<*> get() = MinecartModel

    override fun onRightClick(controls: RemcraftControls, inHand: ItemSlot) {
        val player = controls.player
        player.ridingOnEntity?.exitRiding(player)
        if (rider == null) startRiding(player)
    }
}