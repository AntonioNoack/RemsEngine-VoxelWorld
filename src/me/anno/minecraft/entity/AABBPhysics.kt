package me.anno.minecraft.entity

import me.anno.maths.Maths.clamp
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.CustomBlockBounds
import me.anno.minecraft.world.Dimension
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

class AABBPhysics(
    var position: Vector3d,
    var size: Vector3f,
) {

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

    fun step(dimension: Dimension, dt: Float) {

        assertTrue(dt >= 0f)

        acceleration.mulAdd(dt, velocity, velocity)
        velocity.max(minVelocity)
        velocity.min(maxVelocity)

        val px = position.x
        val py = position.y
        val pz = position.z
        val qx = px + velocity.x
        val qy = py + velocity.y
        val qz = pz + velocity.z

        val bounds = AABBd(
            min(px, qx), min(py, qy), min(pz, qz),
            max(px, qx), max(py, qy), max(pz, qz)
        )

        // calculate, which blocks need to be checked
        val x0 = floor(bounds.minX).toInt()
        val x1 = ceil(bounds.maxX).toInt()
        // extend bounds a little for fences
        val y0 = floor(bounds.minY - fenceDy).toInt()
        val y1 = ceil(bounds.maxY).toInt()
        val z0 = floor(bounds.minZ).toInt()
        val z1 = ceil(bounds.maxZ).toInt()

        val blockBounds = AABBd()
        val blockCenter = Vector3d()
        val oldPosition = Vector3d(position)
        position.set(qx, qy, qz)
        for (z in z0 until z1) {
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val block = dimension.getBlockAt(x, y, z)
                    getBlockBounds(x, y, z, block, blockBounds)
                    if (blockBounds.testAABB(bounds)) {
                        blockBounds.getCenter(blockCenter)
                        // overlap -> we need to check, and potentially stop the block from falling
                        for (dim in 0 until 3) {
                            val p0 = oldPosition[dim]
                            val p1 = position[dim]
                            val vSign = sign(velocity[dim])
                            if (vSign.toDouble() == sign(blockCenter[dim] - p1)) {
                                // we need to clamp :)
                                val p2 = blockBounds.getMinOrMax(dim, vSign > 0f, size[dim] * 0.5)
                                position[dim] = clamp(p1, min(p0, p2), max(p0, p2))
                                velocity[dim] = 0f
                            }
                        }
                    }
                }
            }
        }
    }

    fun applyFriction(dt: Float) {
        velocity.mul(exp(-friction * dt))
    }

    private fun AABBd.getMinOrMax(dim: Int, isMin: Boolean, halfExtends: Double): Double {
        return if (isMin) getMin(dim) - halfExtends else getMax(dim) + halfExtends
    }
}