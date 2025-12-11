package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.components.FillSpace
import me.anno.ecs.interfaces.Renderable
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3f

abstract class Entity(val size: Vector3f) : Component(), FillSpace, Renderable {

    // todo animations, behaviour, ...

    fun removeEntity() {
        val entity = entity ?: return
        entity.destroy()
    }

    private val transforms = ArrayList<Transform>()
    fun getTransform(index: Int): Transform {
        val self = entity!!
        while (index >= transforms.size) {
            val tr = Transform()
            tr.parentEntity = self
            transforms.add(tr)
        }
        return transforms[index]
    }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        val dx = size.x * 0.5
        val dy = size.y * 0.5
        val dz = size.z * 0.5
        dstUnion
            .union(globalTransform.m30 - dx, globalTransform.m31 - dy, globalTransform.m32 - dz)
            .union(globalTransform.m30 + dx, globalTransform.m31 + dy, globalTransform.m32 + dz)
    }
}