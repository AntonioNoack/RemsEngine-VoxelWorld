package me.anno.minecraft.entity.physics

import me.anno.graph.octtree.OctTree
import me.anno.minecraft.entity.MovingEntity
import org.joml.Vector3d

/**
 * Acceleration structure for finding colliding entities
 * */
class EntityTree : OctTree<MovingEntity>(16) {
    override fun createChild() = EntityTree()
    override fun getMin(data: MovingEntity): Vector3d = data.minPosition
    override fun getMax(data: MovingEntity): Vector3d = data.maxPosition
}