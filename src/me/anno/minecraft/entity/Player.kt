package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.minecraft.multiplayer.NetworkData
import org.joml.Vector3f

class Player(var isPrimary: Boolean, name: String) : MovingEntity(playerSize) {

    constructor() : this(false, "Gustav${(Math.random() * 1e6).toInt()}")

    companion object {
        private val playerSize = Vector3f(0.6f, 1.8f, 0.6f)
    }

    init {
        this.name = name
    }

    val networkData = NetworkData()
    var spectatorMode = false
    var bodyRotationY = 0f
    var headRotationX = 0f

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