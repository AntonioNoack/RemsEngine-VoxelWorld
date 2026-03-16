package me.anno.remcraft.ui.controls

import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.builder.DetailedBlockMesh.Companion.DETAIL_SIZE
import me.anno.remcraft.block.types.DetailedBlockVisuals
import me.anno.remcraft.entity.Animal
import me.anno.remcraft.entity.PlayerEntity
import me.anno.remcraft.entity.physics.CollisionSystem
import me.anno.remcraft.world.Dimension
import org.joml.AABBi
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object Raycast {

    private val detailBounds = AABBi()
    fun clickCast(query: RayQuery, player: PlayerEntity?, dimension: Dimension): RayQuery? {
        // find, which block was clicked
        // expensive way, using raycasting:

        val start = query.start
        val end = query.end
        val min = start.min(end, Vector3d())
        val max = start.max(end, Vector3d())
        val dir = query.direction

        val localPos = Vector3f()
        val localDir = Vector3f()
        val tmpM = Matrix4x3()
        CollisionSystem.animals.query(min, max) { target ->
            if (target != player && (target !is PlayerEntity || !target.gameMode.isGhost())) {

                target.model.fill(target.transform!!) { mesh, transform ->
                    transform.validate()

                    val gt = transform.globalTransform
                    localPos.set(start)

                    val gti = gt.invert(tmpM)
                    gti.transformDirection(dir, localDir)
                    gti.transformPosition(localPos)

                    val distance = mesh.getBounds()
                        .whereIsRayIntersecting(
                            localPos.x, localPos.y, localPos.z,
                            1f / localDir.x, 1f / localDir.y, 1f / localDir.z,
                            0f
                        ).toDouble()

                    if (distance < query.result.distance) {
                        query.result.distance = distance
                        query.result.shadingNormalWS.set(dir)
                        query.result.geometryNormalWS.set(dir)
                        query.result.component = target
                    }
                }
            }

            false
        }

        // todo bug: there is regions, where our cursor no longer works :(
        //  -> it skips blocks???

        if (query.result.component is Animal) {
            val dist = query.result.distance
            end.set(start.x + dir.x * dist, start.y + dir.y * dist, start.z + dir.z * dist)
        }

        val queryBounds = AABBi(
            floor(min(start.x, end.x)).toInt(),
            floor(min(start.x, end.y)).toInt(),
            floor(min(start.x, end.z)).toInt(),

            ceil(max(start.x, end.x)).toInt(),
            ceil(max(start.x, end.y)).toInt(),
            ceil(max(start.x, end.z)).toInt(),
        )

        val hitBlock =
            BlockTracing.blockTrace(query, (query.result.distance * 3).toInt(), queryBounds) { xi, yi, zi ->
                val block = dimension.getBlockAt(xi, yi, zi) ?: BlockRegistry.Air
                val hit = if (block is DetailedBlockVisuals) {

                    // todo test this code...

                    start.mul(DETAIL_SIZE.toDouble())
                    query.result.distance *= DETAIL_SIZE

                    detailBounds
                        .setMin(xi * 16, yi * 16, zi * 16)
                        .setMax(xi * 16 + 16, yi * 16 + 16, zi * 16 + 16)

                    val detail = block.getModel()
                    val hitDetails = BlockTracing.blockTrace(query, DETAIL_SIZE * 3, detailBounds) { xi, yi, zi ->
                        if (detail.getVoxel(xi, yi, zi)) BlockTracing.SOLID_BLOCK
                        else BlockTracing.AIR_BLOCK
                    }

                    start.mul(1.0 / DETAIL_SIZE)
                    query.result.distance *= 1.0 / DETAIL_SIZE

                    if (hitDetails) {
                        query.result.positionWS.mul(1.0 / DETAIL_SIZE)
                    }

                    hitDetails
                } else block.isSolid
                if (hit) BlockTracing.SOLID_BLOCK
                else BlockTracing.AIR_BLOCK
            }

        if (hitBlock) {
            query.result.component = null
        }

        return if (hitBlock || query.result.component != null) query else null
    }
}