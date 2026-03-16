package me.anno.remcraft.entity

import me.anno.Time
import me.anno.io.base.BaseWriter
import me.anno.remcraft.entity.model.Model
import me.anno.remcraft.entity.model.PlaneCreator
import me.anno.remcraft.entity.model.XpModel
import me.anno.utils.OS.res
import me.anno.utils.types.AnyToInt.getInt
import org.joml.Vector3f

class XpOrbEntity(var value: Int) : MovingEntity(halfExtents, blockTexture) {
    @Suppress("unused")
    constructor() : this(1)

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

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("value", value)
    }

    override fun setProperty(name: String, value: Any?) {
        if (name == "value") this.value = getInt(value, 1)
        else super.setProperty(name, value)
    }

}