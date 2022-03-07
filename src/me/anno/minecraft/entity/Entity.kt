package me.anno.minecraft.entity

import me.anno.ecs.Component

open class Entity : Component() {

    // todo animations, behaviour, ...

    override fun clone(): Component {
        val clone = Entity()
        copy(clone)
        return clone
    }

}