package me.anno.minecraft.entity

import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo10
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.CustomBlockBounds
import me.anno.minecraft.world.Dimension
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

class AABBPhysics(val position: Vector3d, val size: Vector3f) {

    companion object {
        private val minVelocity = Vector3f(-0.9999f)
        private val maxVelocity = Vector3f(+0.9999f)
        private val defaultBlockSize = AABBf(0f, 0f, 0f, 1f, 1f, 1f)

        fun getBlockBounds(x: Int, y: Int, z: Int, block: BlockType, dst: AABBd): AABBd {
            val cx = x.toDouble()
            val cy = y.toDouble()
            val cz = z.toDouble()
            val size = (block as? CustomBlockBounds)?.customSize ?: defaultBlockSize
            return dst.set(size).translate(cx, cy, cz)
        }

        val fenceDy = 0.5
    }

    val velocity = Vector3f()
    val acceleration = Vector3f()
    var friction = 5f

    var stepHeight = 0.5f

    fun step(dimension: Dimension, dt: Float) {

        assertTrue(dt >= 0f)

        acceleration.mulAdd(dt, velocity, velocity)
        velocity.max(minVelocity)
        velocity.min(maxVelocity)

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

        for (dim in 0 until 3) {

            oldPosition.set(position)

            // todo implement step-height somehow...
            // oldPosition.y += stepHeight

            position[dim] += velocity[dim]

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

                            if (dim == 1) {
                                DebugShapes.debugAABBs.add(DebugAABB(AABBd(blockBounds), 0x777777 or black, 0f))
                            }

                            if (blockBounds.testAABB(entityBounds)) {
                                blockBounds.getCenter(blockCenter)
                                // overlap -> we need to check, and potentially stop the block from falling
                                if (vSign == sign(blockCenter[dim] - newPosI)) {
                                    // we need to clamp :)
                                    val clampPosI = blockBounds.getMinOrMax(dim, vSign > 0.0, selfSize[dim] * 0.5)
                                    position[dim] = clamp(newPosI, min(oldPosI, clampPosI), max(oldPosI, clampPosI))
                                    velocity[dim] = 0f
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
    }

    fun applyFriction(dt: Float) {
        velocity.mul(dtTo10(dt * friction))
    }

    private fun AABBd.getMinOrMax(dim: Int, isMin: Boolean, halfExtends: Double): Double {
        return if (isMin) getMin(dim) - halfExtends else getMax(dim) + halfExtends
    }
}