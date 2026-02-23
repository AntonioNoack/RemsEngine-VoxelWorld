package me.anno.minecraft.entity

import me.anno.Time
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PlaneCreator
import me.anno.minecraft.entity.model.XpModel
import me.anno.utils.OS.res
import org.joml.Vector3f

class XpOrbEntity(val value: Int) : MovingEntity(halfExtents, blockTexture) {

    companion object {
        private val halfExtents = Vector3f(0.2f)
        private val blockTexture = Texture(res.getChild("textures/utils/StarsXp.png")).apply {
            material.emissiveMap = material.diffuseMap
            material.emissiveBase.set(3f)
        }

        private const val HALF_SIZE = 0.2f
        private val models = listOf(
            PlaneCreator.createPlane(0.0f, 0.0f, 0.5f, 0.5f, HALF_SIZE),
            PlaneCreator.createPlane(0.0f, 0.5f, 0.5f, 1.0f, HALF_SIZE),
            PlaneCreator.createPlane(0.5f, 0.0f, 1.0f, 0.5f, HALF_SIZE),
            PlaneCreator.createPlane(0.5f, 0.5f, 1.0f, 1.0f, HALF_SIZE),
        ).map { mesh -> XpModel(mesh) }
    }

    var spawnTime = Time.gameTime

    override val model: Model<*>
        get() = models[System.identityHashCode(this) and 3]

}