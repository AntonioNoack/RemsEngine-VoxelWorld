package me.anno.remcraft.entity

import me.anno.ecs.systems.OnUpdate
import me.anno.remcraft.entity.model.ArrowModel
import me.anno.remcraft.entity.model.Model
import me.anno.remcraft.entity.physics.CollisionSystem
import me.anno.utils.OS.res
import org.joml.Vector3d
import org.joml.Vector3f

class ArrowEntity : MovingEntity(halfExtents, texture), OnUpdate {

    companion object {
        private val halfExtents = Vector3f(0.2f)
        private val texture = Texture(res.getChild("textures/tools/Arrow.png"))
    }

    override val model: Model<*> get() = ArrowModel

    override fun onUpdate() {
        // todo despawn after some time(?)
        // todo skip the check for "self" for a few frames? or start outside of body?

        val min = Vector3d(position).sub(0.2)
        val max = Vector3d(position).add(0.2)
        val collidedWith = CollisionSystem.animals.query(min, max) { other -> other !== this }
        if (collidedWith != null) {
            // todo if is Animal, deal damage
            // todo attach to entity, and stick to it
            // destroyEntity()
        }
    }

}