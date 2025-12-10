package me.anno.minecraft.entity

import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo10
import me.anno.maths.Maths.sq
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.CustomBlockBounds
import me.anno.minecraft.world.Dimension
import me.anno.utils.Color.black
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

class AABBPhysics(val position: Vector3d, val size: Vector3f) {

    companion object {

        private val maxStep = 0.99f

        private val defaultBlockSize = AABBf(0f, 0f, 0f, 1f, 1f, 1f)

        fun getBlockBounds(x: Int, y: Int, z: Int, block: BlockType, dst: AABBd): AABBd {
            val cx = x.toDouble()
            val cy = y.toDouble()
            val cz = z.toDouble()
            val size = (block as? CustomBlockBounds)?.customSize ?: defaultBlockSize
            return dst.set(size).translate(cx, cy, cz)
        }

        val fenceDy = 0.5
        val airFriction = 0.2f
    }

    val actualVelocity = Vector3f()
    val targetVelocity = Vector3f()
    val acceleration = Vector3f()

    val prevPosition = Vector3d()

    val isOnGround: Boolean
        get() = actualVelocity.y == 0f

    var friction = airFriction
    var stepHeight = 0.5f

    fun updateVelocity(dt: Float) {
        targetVelocity.fma(dt, acceleration)
        prevPosition.set(position)
    }

    fun updateActualVelocity(dt: Float) {
        check(dt != 0f)
        position.sub(prevPosition, actualVelocity).mul(1f / dt)
        targetVelocity.set(actualVelocity)
    }

    fun stepSpectator(dt: Float) {
        updateVelocity(dt)
        position.fma(dt, targetVelocity)
        actualVelocity.set(targetVelocity)
    }

    fun step(dimension: Dimension, dt: Float) {

        updateVelocity(dt)

        val px = position.x
        val py = position.y
        val pz = position.z

        val selfSize = size
        val selfDx = 0.5 * selfSize.x
        val selfDy = 0.5 * selfSize.y
        val selfDz = 0.5 * selfSize.z

        val entityBounds = AABBd()
        val blockBounds = AABBd()
        val blockCenter = Vector3d()
        val oldPosition = Vector3d()

        var bestFloorBlock: BlockType = BlockRegistry.Air
        var bestFloorScore = Double.POSITIVE_INFINITY

        for (dim in 0 until 3) {

            oldPosition.set(position)

            // todo implement step-height somehow...
            // oldPosition.y += stepHeight

            position[dim] += clamp(targetVelocity[dim] * dt, -maxStep, maxStep)

            val qx = position.x
            val qy = position.y
            val qz = position.z
            entityBounds.setMin(
                min(px, qx) - selfDx,
                min(py, qy) - selfDy,
                min(pz, qz) - selfDz
            ).setMax(
                max(px, qx) + selfDx,
                max(py, qy) + selfDy,
                max(pz, qz) + selfDz
            )

            if (dim == 1) {
                DebugShapes.debugAABBs.add(
                    DebugAABB(
                        AABBd(
                            px - selfDz, py - selfDy, pz - selfDz,
                            px + selfDx, py + selfDy, pz + selfDz
                        ), -1, 0f
                    )
                )
            }

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

            fun check(dy: Float) {
                val oldPosI = oldPosition[dim]
                val newPosI = position[dim] + dy
                val vSign = sign(newPosI - oldPosI)
                if (vSign != 0.0) search@ for (z in z0 until z1) {
                    for (y in y0 until y1) {
                        for (x in x0 until x1) {
                            val block = dimension.getBlockAt(x, y, z)
                            getBlockBounds(x, y, z, block, blockBounds)

                            // todo is this correct, or do we need to add 0.5?
                            if (block != BlockRegistry.Air) {
                                val floorScore = sq(qx - x, qz - z) + (y - qy)
                                if (floorScore < bestFloorScore) {
                                    bestFloorBlock = block
                                    bestFloorScore = floorScore
                                }
                            }

                            if (dim == 1) {
                                // debug-show what we're colliding with
                                DebugShapes.debugAABBs.add(DebugAABB(AABBd(blockBounds), 0x777777 or black, 0f))
                            }

                            if (blockBounds.testAABB(entityBounds)) {
                                blockBounds.getCenter(blockCenter)
                                // overlap -> we need to check, and potentially stop the block from falling
                                if (vSign == sign(blockCenter[dim] - newPosI)) {
                                    // we need to clamp :)
                                    val clampPosI = blockBounds.getMinOrMax(dim, vSign > 0.0, selfSize[dim] * 0.5)
                                    position[dim] = clamp(newPosI, min(oldPosI, clampPosI), max(oldPosI, clampPosI))
                                    break@search
                                }
                            }
                        }
                    }
                }
            }

            check(0f)
            if (dim == 1) check(stepHeight)

        }

        if (dt != 0f) {
            updateActualVelocity(dt)
        }

        friction =
            if (isOnGround) bestFloorBlock.friction
            else airFriction
    }

    fun applyFriction(dt: Float) {
        targetVelocity.mul(dtTo10(dt * friction))
    }

    private fun AABBd.getMinOrMax(dim: Int, isMin: Boolean, halfExtends: Double): Double {
        return if (isMin) getMin(dim) - halfExtends else getMax(dim) + halfExtends
    }
}