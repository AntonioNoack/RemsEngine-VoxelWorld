package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.minecraft.multiplayer.NetworkData

class Player(var isPrimary: Boolean, name: String) : Entity() {

    constructor() : this(false, "Gustav${(Math.random() * 1e6).toInt()}")

    init {
        this.name = name
    }

    val networkData = NetworkData()

    override val className: String = "MCPlayer"

    override fun clone(): Component {
        val clone = Player(isPrimary, name)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Player
        clone.isPrimary = isPrimary
    }

}