package me.anno.minecraft.entity

import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Quaternionf
import org.joml.Vector3f

abstract class Animal(halfExtents: Vector3f) : MovingEntity(halfExtents) {

    val bodyRotation: Quaternionf get() = transform!!.localRotation
    val headRotation = Quaternionf()

    val bodyRotationY get() = bodyRotation.getEulerAngleYXZvY()
    val headRotationX get() = headRotation.getEulerAngleYXZvX()

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Animal
        dst.headRotation.set(headRotation)
    }
}