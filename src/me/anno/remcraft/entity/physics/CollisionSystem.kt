package me.anno.remcraft.entity.physics

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.posMod
import me.anno.remcraft.audio.playItemCollectSound
import me.anno.remcraft.audio.playXpCollectSound
import me.anno.remcraft.coasters.CoasterRail
import me.anno.remcraft.coasters.Minecart
import me.anno.remcraft.entity.*
import me.anno.ui.UIColors
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.sign

object CollisionSystem : System(), OnUpdate {

    // todo merge items of the same type

    val physicsDt = 1f / 30f

    val xpAttractExtents = Vector3f(8f)
    val xpCollectExtents = Vector3f(0.2f)

    val itemCollectExtents = Vector3f(1f, 1f, 1f)

    val animals = EntityTree<MovingEntity>()
    val items = EntityTree<ItemEntity>()
    val xp = EntityTree<XpOrbEntity>()
    val rails = EntityTree<CoasterRail>()

    private val remove = ArrayList<RemcraftEntity>()

    private val oldMin = Vector3d()
    private val oldMax = Vector3d()

    override fun setContains(component: Component, contains: Boolean) {
        when (component) {
            is XpOrbEntity -> xp.setContains(component, contains)
            is ItemEntity -> items.setContains(component, contains)
            is PlayerEntity, is Animal, is Minecart ->
                animals.setContains(component, contains)
            is CoasterRail -> rails.setContains(component, contains)
        }
    }

    var remainingTime = 0f

    override fun onUpdate() {
        // update depending on passed time & physicsDt...
        remainingTime += Time.deltaTime.toFloat()
        while (remainingTime > 0f) {
            remainingTime -= physicsDt
            updateSubStep()
        }
    }

    fun updateSubStep() {
        animalItemCollection()
        playerXpAttraction()
        playerXpCollection()
        entityEntityRepulsion()
        keepOnRails()
        removeEntities()
        updatePhysics()
    }

    fun updatePhysics() {
        animals.forEach { animal -> animal.stepPhysics(physicsDt) }
        animals.forEach { animal -> animal.applyRiding() }
        animals.forEach { animal -> animal.invalidateBounds() }
    }

    fun removeEntities() {
        for (item in remove) {
            item.removeFromWorld()
        }
        remove.clone()
    }

    fun keepOnRails() {
        animals.queryPairs(0, rails) { animal, rail ->
            if (!animal.isRemoved) applyRailCollisionForces(animal, rail)
            false
        }
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

    private fun applyRailCollisionForces(moving: MovingEntity, rail: CoasterRail) {
        // apply physics from rail onto animal/minecart
        val t = rail.findClosestT(moving.position)
        if (t !in 0.0..1.0) return

        val railSpace = rail.getTransformAt(t, Matrix4x3())
        if (false) {
            val v0 = railSpace.transformPosition(Vector3d())
            val vx = railSpace.transformPosition(Vector3d().apply { x = 1.0 })
            val vy = railSpace.transformPosition(Vector3d().apply { y = 1.0 })
            val vz = railSpace.transformPosition(Vector3d().apply { z = 1.0 })

            DebugShapes.showDebugArrow(DebugLine(v0, vx, UIColors.axisXColor, 0f))
            DebugShapes.showDebugArrow(DebugLine(v0, vy, UIColors.axisYColor, 0f))
            DebugShapes.showDebugArrow(DebugLine(v0, vz, UIColors.axisZColor, 0f))
        }

        val railSpaceInv = railSpace.invert(Matrix4x3())
        // transform position and velocity from global space into rail space
        val globalPos = moving.position
        val localPos = railSpaceInv.transformPosition(globalPos, Vector3d())
        val globalVel = moving.physics.targetVelocity
        val localVel = railSpaceInv.transformDirection(globalVel, Vector3f())
        val globalAcc = moving.physics.acceleration
        val localAcc = railSpaceInv.transformDirection(globalAcc, Vector3f())
        val dt = physicsDt // Time.deltaTime

        // add height-offset to localPos...
        localPos.y -= moving.halfExtents.y

        // if is close enough to rail & right-side up, keep it inside the rail...
        if (localPos.lengthSquared() > 10f) return // too far away

        // prevent a from falling through floor
        if (localPos.y + dt * localVel.y <= 0.0) {
            localVel.y = max(localVel.y, 0f)
            localAcc.y = (-localPos.y / dt).toFloat()
        }

        // keep it in the center
        if (moving is Minecart) {
            val v = localVel.length()
            localVel.x = 0.5f * (-localPos.x / dt).toFloat()
            localAcc.x = 0.5f * (localVel.x / dt)
            localVel.safeNormalize(v)

            val at = moving.transform!!
            // todo is an x-z-swap the solution???
            at.localRotation = railSpace.getNormalizedRotation(at.localRotation).normalize()
            val animWheelSpeed = 7f
            moving.animPosition = posMod(moving.animPosition + animWheelSpeed * localVel.z * dt, TAUf)
        }

        railSpace.transformDirection(localVel, globalVel)
        railSpace.transformDirection(localAcc, globalAcc)
    }

    private fun <V : MovingEntity> V.updatePositions(halfExtents: Vector3f, tree: EntityTree<V>) {
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