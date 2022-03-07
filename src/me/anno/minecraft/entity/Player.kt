package me.anno.minecraft.entity

import me.anno.ecs.Component

class Player : Entity() {

    override val className: String = "MCPlayer"

    override fun clone(): Component {
        val clone = Player()
        copy(clone)
        return clone
    }

}