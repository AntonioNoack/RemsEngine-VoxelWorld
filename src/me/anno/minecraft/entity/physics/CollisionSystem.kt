package me.anno.minecraft.entity.physics

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.minecraft.entity.ItemEntity
import me.anno.minecraft.entity.MovingEntity
import org.joml.Vector3d
import org.joml.Vector3f

object CollisionSystem : System(), OnUpdate {

    val tree = EntityTree()

    private val oldMin = Vector3d()
    private val oldMax = Vector3d()

    override fun setContains(component: Component, contains: Boolean) {
        if (component is MovingEntity && component !is ItemEntity) {
            if (contains) {
                tree.add(component)
            } else {
                tree.remove(component)
            }
        }
    }

    override fun onUpdate() {
        tree.forEach { it.updatePositions() }
        tree.queryPairs(0) { a, b ->
            applyCollisionForces(a, b)
            false
        }
    }

    private val dir = Vector3f()
    private fun applyCollisionForces(a: MovingEntity, b: MovingEntity) {
        val ap = a.physics
        val bp = b.physics
        ap.position.sub(bp.position, dir)
        val strength = 30f / (1f + dir.lengthSquared())
        ap.acceleration.fma(+strength, dir)
        bp.acceleration.fma(-strength, dir)
    }

    private fun MovingEntity.updatePositions() {
        val dx = halfExtents.x * 0.5
        val dy = halfExtents.y * 0.5
        val dz = halfExtents.z * 0.5
        val pos = physics.position
        oldMin.set(minPosition)
        oldMax.set(maxPosition)
        minPosition.set(pos.x - dx, pos.y - dy, pos.z - dz)
        maxPosition.set(pos.x + dx, pos.y + dy, pos.z + dz)
        tree.update(this, oldMin, oldMax)
    }

}