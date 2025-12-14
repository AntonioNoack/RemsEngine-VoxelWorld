package me.anno.minecraft.entity.physics

import me.anno.graph.octtree.OctTree
import me.anno.minecraft.entity.MovingEntity
import org.joml.Vector3d

/**
 * Acceleration structure for finding colliding entities
 * */
class EntityTree<Type: MovingEntity> : OctTree<Type>(16) {
    override fun createChild() = EntityTree<Type>()
    override fun getMin(data: Type): Vector3d = data.minPosition
    override fun getMax(data: Type): Vector3d = data.maxPosition
}