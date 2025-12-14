package me.anno.minecraft.entity.physics

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.minecraft.entity.ItemEntity
import me.anno.minecraft.entity.MovingEntity
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.entity.XpOrbEntity
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sign

object CollisionSystem : System(), OnUpdate {

    // todo merge items of the same type
    // todo merge nearby xp

    val xpAttractExtents = Vector3f(5f)

    val tree = EntityTree<MovingEntity>()
    val items = EntityTree<ItemEntity>()
    val xp = EntityTree<XpOrbEntity>()

    private val oldMin = Vector3d()
    private val oldMax = Vector3d()

    override fun setContains(component: Component, contains: Boolean) {
        when (component) {
            is XpOrbEntity -> xp.setContains(component, contains)
            is ItemEntity -> items.setContains(component, contains)
            is MovingEntity -> tree.setContains(component, contains)
        }
    }

    private fun <V : MovingEntity> EntityTree<V>.setContains(value: V, contains: Boolean) {
        if (contains) {
            add(value)
        } else {
            remove(value)
        }
    }

    override fun onUpdate() {
        animalItemCollection()
        playerXpAttraction()
        entityEntityRepulsion()
    }

    fun animalItemCollection() {}

    fun playerXpAttraction() {
        xp.forEach { it.updatePositions(xpAttractExtents, xp) }
        tree.queryPairs(0, xp) { entity, item ->
            if (entity is PlayerEntity) applyAttractionForce(entity, item)
            false
        }
        // todo if close enough, collect it
    }

    fun entityEntityRepulsion() {
        tree.forEach { it.updatePositions(it.halfExtents, tree) }
        tree.queryPairs(0) { a, b ->
            applyCollisionForces(a, b)
            false
        }
    }

    private fun applyAttractionForce(a: MovingEntity, b: MovingEntity) {
        val ap = a.physics
        val bp = b.physics
        ap.position.sub(bp.position, diff)

        val strength = 10f / (diff.length() + 1e-3f)
        bp.acceleration.fma(+strength, diff)
    }

    private val diff = Vector3f()
    private fun applyCollisionForces(a: MovingEntity, b: MovingEntity) {

        val ap = a.physics
        val bp = b.physics
        ap.position.sub(bp.position, diff)

        val strength = 10f
        val fx = (diff.x - sign(diff.x) * (a.halfExtents.x + b.halfExtents.x)) * strength
        val fy = (diff.y - sign(diff.y) * (a.halfExtents.y + b.halfExtents.y)) * strength
        val fz = (diff.z - sign(diff.z) * (a.halfExtents.z + b.halfExtents.z)) * strength
        ap.acceleration.sub(fx, fy, fz)
        bp.acceleration.add(fx, fy, fz)
    }

    private fun <V : MovingEntity> V.updatePositions(
        halfExtents: Vector3f, tree: EntityTree<V>
    ) {
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