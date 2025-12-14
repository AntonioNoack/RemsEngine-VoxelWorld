package me.anno.minecraft.entity.ai

import me.anno.maths.Maths.sq
import me.anno.minecraft.entity.Entity
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.entity.physics.CollisionSystem
import me.anno.minecraft.rendering.v2.dimension
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3d
import org.joml.Vector3i
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

object FindTargets {

    fun findPlayerTarget(self: Entity, maxDistance: Double): PlayerEntity? {
        val pos = self.position
        val min = Vector3d(pos).sub(maxDistance)
        val max = Vector3d(pos).add(maxDistance)
        var bestFind: PlayerEntity? = null
        var bestDistance = sq(maxDistance)
        CollisionSystem.tree.query(min, max) { target ->
            if (target is PlayerEntity && target !== self) {
                val distance = target.position.distanceSquared(pos)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestFind = target
                }
            }
            false
        }
        return bestFind
    }

    /**
     * try finding a grass block
     * if not, find the best other block
     * */
    fun findGrassyBlock(
        start: Vector3i, maxDistance: Double,
        maxTries: Int, height: Int, wantGrassy: Boolean,
        seed: Long,
    ): Vector3i? {
        val random = Random(seed)
        val maxDistanceI = maxDistance.toInt()

        var bestX = 0
        var bestZ = 0
        var bestY = 0
        var bestScore = Int.MAX_VALUE

        for (i in 0 until maxTries) {
            val xi = start.x + random.nextInt(maxDistanceI)
            val zi = start.z + random.nextInt(maxDistanceI)

            for (yi in start.y - maxDistanceI..start.y + maxDistanceI) {
                if (canStandAt(xi, yi, zi, height)) {
                    val ground = dimension.getBlockAt(xi, yi - 1, zi) ?: continue
                    val isGrassy = ground.isGrassy
                    val score = abs(yi - start.y) + (isGrassy == wantGrassy).toInt(maxDistanceI + 1)
                    if (score < bestScore) {
                        bestScore = score
                        bestX = xi
                        bestY = yi
                        bestZ = zi
                    }
                }
            }

            if (bestScore <= maxDistanceI) break
        }
        return if (bestScore < Int.MAX_VALUE) {
            Vector3i(bestX, bestY, bestZ)
        } else null
    }

    fun getPosition(self: Entity): Vector3i {
        val pos = self.position
        val x = floor(pos.x).toInt()
        val y = floor(pos.y).toInt()
        val z = floor(pos.z).toInt()
        return Vector3i(x, y, z)
    }

    fun canStandAt(x: Int, y: Int, z: Int, height: Int): Boolean {
        val ground = dimension.getBlockAt(x, y - 1, z)
        if (ground == null || !ground.isSolid) return false
        for (yi in y until y + height) {
            val above = dimension.getBlockAt(x, yi, z)
            if (above == null || !above.isWalkable) return false
        }
        return true
    }

}