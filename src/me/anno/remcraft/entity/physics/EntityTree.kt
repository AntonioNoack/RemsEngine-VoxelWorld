package me.anno.remcraft.entity.physics

import me.anno.graph.octtree.OctTree
import me.anno.remcraft.entity.RemcraftEntity
import org.joml.Vector3d

/**
 * Acceleration structure for finding colliding entities
 * */
class EntityTree<Type : RemcraftEntity> : OctTree<Type>(16) {
    override fun createChild() = EntityTree<Type>()
    override fun getMin(data: Type): Vector3d = data.minPosition
    override fun getMax(data: Type): Vector3d = data.maxPosition

    fun setContains(value: Type, contains: Boolean) {
        if (contains) {
            add(value)
        } else {
            remove(value)
        }
    }
}