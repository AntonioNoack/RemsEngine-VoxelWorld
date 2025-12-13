package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.components.FillSpace
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline
import me.anno.minecraft.entity.model.Model
import org.joml.*
import kotlin.math.floor

abstract class Entity(val halfExtents: Vector3f) : Component(), FillSpace, Renderable {

    // todo animations, behaviour, ...

    abstract val model: Model<*>

    override fun fill(pipeline: Pipeline, transform: Transform) {
        @Suppress("UNCHECKED_CAST")
        (model as Model<Entity>).self = this
        model.fill(pipeline, transform)
    }

    fun removeEntity() {
        val entity = entity ?: return
        entity.destroy()
    }

    val position: Vector3d
        get() = transform!!.localPosition

    val rotation: Quaternionf
        get() = transform!!.localRotation

    val blockPosition: Vector3i
        get() {
            val pos = position
            return Vector3i(
                floor(pos.x).toInt(),
                floor(pos.y).toInt(),
                floor(pos.z).toInt()
            )
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
        val dx = halfExtents.x
        val dy = halfExtents.y
        val dz = halfExtents.z
        dstUnion
            .union(globalTransform.m30 - dx, globalTransform.m31 - dy, globalTransform.m32 - dz)
            .union(globalTransform.m30 + dx, globalTransform.m31 + dy, globalTransform.m32 + dz)
    }
}