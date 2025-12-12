package me.anno.minecraft.entity.physics

import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.rendering.v2.dimension
import me.anno.utils.hpc.threadLocal
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

class AABBPhysics(val position: Vector3d, val halfExtents: Vector3f) {

    companion object {

        private val maxStep = 0.99f

        val fenceDy = 0.5
        val airFriction = 0.2f

        // todo make these ThreadLocal
        private class Helper {
            val entityBounds = AABBd()
            val blockBounds = AABBd()
            val blockCenter = Vector3d()
        }

        private val helper = threadLocal { Helper() }
    }

    val actualVelocity = Vector3f()
    val targetVelocity = Vector3f()
    val acceleration = Vector3f()

    val prevPosition = Vector3d()

    val isOnGround: Boolean
        get() = actualVelocity.y == 0f

    var friction = airFriction
    var stepHeight = 0.51f

    fun updateVelocity(dt: Float) {
        targetVelocity.fma(dt, acceleration)
        prevPosition.set(position)
    }

    fun updateActualVelocity(dt: Float) {
        check(dt != 0f)
        position.sub(prevPosition, actualVelocity).mul(1f / dt)
        // targetVelocity.set(actualVelocity)
    }

    fun stepSpectator(dt: Float) {
        updateVelocity(dt)
        position.fma(dt, targetVelocity)
        actualVelocity.set(targetVelocity)
    }

    private fun tryMove(delta: Float, dim: Int): BlockType {
        val vSign = sign(delta).toDouble()
        if (vSign == 0.0) return BlockRegistry.Air

        val (selfDx, selfDy, selfDz) = halfExtents
        val (px, py, pz) = position

        val oldPosI = position[dim]
        position[dim] = oldPosI + Maths.clamp(delta, -maxStep, maxStep)

        val helper = helper.get()
        val (qx, qy, qz) = position
        val entityBounds = helper.entityBounds
        entityBounds.setMin(
            min(px, qx) - selfDx,
            min(py, qy) - selfDy,
            min(pz, qz) - selfDz
        ).setMax(
            max(px, qx) + selfDx,
            max(py, qy) + selfDy,
            max(pz, qz) + selfDz
        )

        val epsilon = -0.01
        entityBounds.addMargin(
            if (dim == 0) 0.0 else epsilon,
            if (dim == 1) 0.0 else epsilon,
            if (dim == 2) 0.0 else epsilon,
        )

        // calculate, which blocks need to be checked
        val x0 = floor(entityBounds.minX).toInt()
        val x1 = ceil(entityBounds.maxX).toInt()
        // extend bounds a little for fences
        val y0 = floor(entityBounds.minY - fenceDy).toInt()
        val y1 = ceil(entityBounds.maxY).toInt()
        val z0 = floor(entityBounds.minZ).toInt()
        val z1 = ceil(entityBounds.maxZ).toInt()

        var bestFloorBlock: BlockType = BlockRegistry.Air
        var bestFloorScore = Double.POSITIVE_INFINITY

        val blockBounds = helper.blockBounds
        var newPosI = position[dim]
        for (z in z0 until z1) {
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val block = dimension.getBlockAt(x, y, z)
                    block.getBounds(x, y, z, blockBounds)

                    // todo is this correct, or do we need to add 0.5?
                    if (dim == 1 && block != BlockRegistry.Air) {
                        val floorScore = Maths.sq(qx - x, qz - z) + (y - qy)
                        if (floorScore < bestFloorScore) {
                            bestFloorBlock = block
                            bestFloorScore = floorScore
                        }
                    }

                    if (blockBounds.testAABB(entityBounds)) {
                        val blockCenter = blockBounds.getCenter(helper.blockCenter)
                        // overlap -> we need to check, and potentially stop the block from falling
                        if (vSign == sign(blockCenter[dim] - newPosI)) {
                            // we need to clamp :)
                            val clampPosI = blockBounds.getMinOrMax(dim, vSign > 0.0, halfExtents[dim].toDouble())
                            newPosI = Maths.clamp(newPosI, min(oldPosI, clampPosI), max(oldPosI, clampPosI))
                        }
                    }
                }
            }
        }

        position[dim] = newPosI

        return bestFloorBlock
    }

    fun step(dt: Float) {
        updateVelocity(dt)

        val stepX = targetVelocity.x * dt
        val stepY = targetVelocity.y * dt
        val stepZ = targetVelocity.z * dt

        tryMove(stepHeight, 1)
        tryMove(stepX, 0)
        tryMove(stepZ, 2)

        val bestFloorBlock = tryMove(stepY - stepHeight, 1)
        if (dt != 0f) {
            updateActualVelocity(dt)
        }

        DebugShapes.debugAABBs.add(
            DebugAABB(
                AABBd().union(position)
                    .addMargin(halfExtents.x.toDouble(), halfExtents.y.toDouble(), halfExtents.z.toDouble()), -1, 0f
            )
        )

        friction =
            if (isOnGround) bestFloorBlock.friction
            else airFriction
    }

    fun applyFriction(dt: Float) {
        targetVelocity.mul(Maths.dtTo10(dt * friction))
    }

    private fun AABBd.getMinOrMax(dim: Int, isMin: Boolean, halfExtents: Double): Double {
        return if (isMin) getMin(dim) - halfExtents else getMax(dim) + halfExtents
    }
}