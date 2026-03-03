package me.anno.minecraft.entity.physics

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.minecraft.audio.playItemCollectSound
import me.anno.minecraft.audio.playXpCollectSound
import me.anno.minecraft.entity.*
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sign

object CollisionSystem : System(), OnUpdate {

    // todo merge items of the same type
    // todo merge nearby xp

    val xpAttractExtents = Vector3f(5f)
    val xpCollectExtents = Vector3f(0.2f)

    val itemCollectExtents = Vector3f(0.7f, 1f, 0.7f)

    val animals = EntityTree<MovingEntity>()
    val items = EntityTree<ItemEntity>()
    val xp = EntityTree<XpOrbEntity>()

    private val remove = ArrayList<Entity>()

    private val oldMin = Vector3d()
    private val oldMax = Vector3d()

    override fun setContains(component: Component, contains: Boolean) {
        when (component) {
            is XpOrbEntity -> xp.setContains(component, contains)
            is ItemEntity -> items.setContains(component, contains)
            is PlayerEntity, is Animal -> animals.setContains(component, contains)
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
        playerXpCollection()
        entityEntityRepulsion()
        removeEntities()
    }

    fun removeEntities() {
        for (item in remove) {
            item.removeFromWorld()
        }
        remove.clone()
    }

    fun animalItemCollection() {
        items.forEach { it.updatePositions(itemCollectExtents, items) }
        animals.queryPairs(0, items) { animal, item ->
            // allow partial collection
            if (!item.isRemoved && animal.addItemFrom(item.stack)) {
                // todo also increase statistics...
                playItemCollectSound(item.position, item.stack.type)
                if (item.stack.count <= 0) remove.add(item)
            }
            false
        }
    }

    fun playerXpCollection() {
        xp.forEach { it.updatePositions(xpCollectExtents, xp) }
        animals.queryPairs(0, xp) { player, xpOrb ->
            if (player is PlayerEntity && !xpOrb.isRemoved) {
                // todo also increase statistics...
                playXpCollectSound(xpOrb.position, xpOrb.value)
                player.addXp(xpOrb.value)
                remove.add(xpOrb)
            }
            false
        }
    }

    fun playerXpAttraction() {
        xp.forEach { it.updatePositions(xpAttractExtents, xp) }
        animals.queryPairs(0, xp) { player, xpOrb ->
            if (player is PlayerEntity) applyAttractionForce(player, xpOrb)
            false
        }
    }

    fun entityEntityRepulsion() {
        animals.forEach { it.updatePositions(it.halfExtents, animals) }
        animals.queryPairs(0) { a, b ->
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

        // todo if strength too high, apply damage
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