package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PlayerModel
import me.anno.minecraft.multiplayer.NetworkData
import me.anno.utils.OS.res
import org.joml.Vector3f
import org.joml.Vector3i

class PlayerEntity(var isPrimary: Boolean, name: String) : Animal(halfExtents, texture) {

    constructor() : this(false, "Gustav${(Math.random() * 1e6).toInt()}")

    companion object {
        private val halfExtents = Vector3f(6f / 16f, 1f, 6f / 16f)
        private val femaleModel = PlayerModel(false)
        private val texture = Texture(res.getChild("textures/players/Reyviee.png"))
    }

    init {
        this.name = name
    }

    override val model: Model<*> get() = femaleModel
    override val maxJumpDown: Int get() = 3
    override val maxHealth: Int get() = 20

    // idk what players would be looking for ;)
    override fun findTarget(start: Vector3i, seed: Long): Vector3i? = null

    val networkData = NetworkData()
    var spectatorMode = false

    var targetHeadY = 0f

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