package me.anno.minecraft.coasters

import me.anno.io.files.InvalidRef
import me.anno.minecraft.entity.Entity
import me.anno.minecraft.entity.Texture
import me.anno.minecraft.entity.model.Model
import org.joml.Vector3f

// todo rideable
// todo slide on tracks until the end:
//  t = estimate progress from tracks
//  pos += sign(vel * dir(t)) * dir(t) * |vel| * dt
//  energy = vel*vel + prevPos.y * C
//  vel = sqrt(max(0, energy - pos.y * C))
// todo bump if stopped
class Minecart : Entity(Vector3f(0.5f), Texture(InvalidRef)) {
    override val model: Model<*>
        get() = TODO("Not yet implemented")
}