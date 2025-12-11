package me.anno.minecraft.entity

import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Quaternionf
import org.joml.Vector3f

abstract class Animal(size: Vector3f) : MovingEntity(size) {

    val bodyRotation: Quaternionf get() = transform!!.localRotation
    val headRotation = Quaternionf()

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Animal
        dst.headRotation.set(headRotation)
    }
}