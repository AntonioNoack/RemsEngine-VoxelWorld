package me.anno.remcraft.rendering.globalillumination

import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.rendering.v2.ChunkIndex.encodeChunkIndex
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Dimension
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.Index.totalSize
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.types.Booleans.hasFlag
import org.joml.AABBi
import org.joml.Vector3d
import org.joml.Vector3f
import speiger.primitivecollections.LongToIntHashMap
import java.util.*
import kotlin.math.*

class GlobalIllumination(val dimension: Dimension) {
    companion object {
        fun decodeX(hash: Long): Int = hash.shl(16).toInt().shr(16)
        fun decodeY(hash: Long): Int = hash.toInt().shr(16)
        fun decodeZ(hash: Long): Int = hash.shr(16).toInt().shr(16)
        fun decodeSideId(hash: Long): Int = hash.shr(48).toInt()
        fun decodeSide(hash: Long): BlockSide = BlockSide.entries[decodeSideId(hash)]

        fun encodeSide(x: Int, y: Int, z: Int, side: BlockSide): Long {
            val xi = x.and(0xffff).toLong()
            val yi = y.and(0xffff).toLong()
            val zi = z.and(0xffff).toLong()
            val si = side.ordinal.toLong()
            return xi + yi.shl(16) + zi.shl(32) + si.shl(48)
        }
    }

    // todo perfect graphics?
    //  a) we can do block-tracing in real-time -> we can trace a ray to the sun if need-be
    //  b) complex:
    //  1st degree via shadow map, and explicit light-source->block-face transfer functions
    //  2nd degree via face->face transfer functions (w < 1, decay with distance, angle, ... max 7 radius)
    //  three or so iterations of 2nd degree; all is diffuse
    //  blur neighboring parallel faces: our rendering needs to know what face we're on, and what our neighbors are

    // todo this is very complex :/
    //  we need the following:
    //  - dynamic buffer of all faces
    //    - pos
    //    - side
    //    - transfer functions (offset, count?) -> to all other buffers = invalidation complex
    //  - 1st degree & 2nd degree compute shaders
    //  - using light data in rendering (each storing face index & neighbors)

    // todo lights can be explicitly modelled as a cpu-computed list
    //  light-level (flicker for flame) * transmission * faceList

    // todo according to sb on Reddit, we only need ~20 random faces per face, and 7 iterations for it to get stable
    // todo this is called Radiosity

    val faces = LongToIntHashMap(1 shl 16)
    fun getFace(x: Int, y: Int, z: Int, side: BlockSide): Int {
        return faces.getOrPut(encodeSide(x, y, z, side)) { faces.size }
    }

    private val raysPerFace = 50
    private val maxDistance = cbrt(totalSize.toFloat())
    private val query = RayQuery(Vector3d(), Vector3f(0f, 0f, 1f), 1e6)

    val connections = PackedIntF3Lists(1 shl 16, raysPerFace, -1)
    val skyEffect = FloatArrayList(1 shl 16)

    fun addChunk(chunk: Chunk) {
        // for each solid face,
        //  find target faces,
        //  list them,
        //  calculate UVs in sun/shadow-space?

        val maxSteps = ceil(maxDistance * 3).toInt()
        val bounds = AABBi().all()

        val baseWeight = 1f / raysPerFace
        val random = Random(encodeChunkIndex(chunk.xi, chunk.yi, chunk.zi))
        forEachFace(chunk) { x, y, z, side ->
            val gx = x + chunk.x0
            val gy = y + chunk.y0
            val gz = z + chunk.z0

            val selfFace = getFace(gx, gy, gz, side)
            val selfBlock = dimension.getBlockAt(gx, gy, gz)!!
            val selfColor = selfBlock.color
            var selfR = selfColor.r01()
            var selfG = selfColor.g01()
            var selfB = selfColor.b01()
            val selfC = (selfR + selfG + selfB) / 3f
            val grayness = 0.7f
            selfR = mix(selfR, selfC, grayness)
            selfG = mix(selfG, selfC, grayness)
            selfB = mix(selfB, selfC, grayness)

            // calculate r,g,b contribution
            var numSkyHits = 0
            repeat(raysPerFace) {
                // some random du,dv for the face
                val du = random.nextFloat() - 0.5f
                val dv = random.nextFloat() - 0.5f

                val posX = getPosX(gx, side, du, dv) + side.x * 0.1f
                val posY = getPosY(gy, side, du, dv) + side.y * 0.1f
                val posZ = getPosZ(gz, side, du, dv) + side.z * 0.1f

                val dirX = side.x + random.nextFloat()
                val dirY = side.y + random.nextFloat()
                val dirZ = side.z + random.nextFloat()

                val query = query
                query.start.set(posX, posY, posZ)
                query.direction.set(dirX, dirY, dirZ).normalize()
                query.end.set(query.start).fma(maxDistance, query.direction)
                query.result.distance = maxDistance.toDouble()

                val hit = BlockTracing.blockTrace(query, maxSteps, bounds) { xi, yi, zi ->
                    val hit = dimension.getBlockAt(xi, yi, zi)!!.isSolid
                    if (hit) BlockTracing.SOLID_BLOCK
                    else BlockTracing.AIR_BLOCK
                }

                if (hit) {
                    val res = query.result
                    val nor = res.geometryNormalWS
                    val ox = floor(res.positionWS.x - nor.x * 0.001).toInt()
                    val oy = floor(res.positionWS.y - nor.y * 0.001).toInt()
                    val oz = floor(res.positionWS.z - nor.z * 0.001).toInt()

                    val otherSide = when {
                        nor.x < -0.5f -> BlockSide.NX
                        nor.y < -0.5f -> BlockSide.NY
                        nor.z < -0.5f -> BlockSide.NZ
                        nor.x > 0.5f -> BlockSide.PX
                        nor.y > 0.5f -> BlockSide.PY
                        nor.z > 0.5f -> BlockSide.PZ
                        else -> throw IllegalArgumentException()
                    }

                    val otherFace = getFace(ox, oy, oz, otherSide)
                    val weight = baseWeight / max(1f, sq(res.distance).toFloat()) *
                            abs(nor.dot(dirX, dirY, dirZ)) // [0,1]
                    if (selfFace >= connections.size) {
                        connections.resizeTo(selfFace * 2)
                    }
                    connections.addUnique(
                        selfFace, otherFace,
                        weight * selfR, weight * selfG, weight * selfB
                    )
                } else numSkyHits++
            }

            skyEffect[selfFace] = numSkyHits * baseWeight
        }
    }

    fun interface ForEachFaceCallback {
        fun call(x: Int, y: Int, z: Int, side: BlockSide)
    }

    fun forEachFace(chunk: Chunk, callback: ForEachFaceCallback) {
        for (z in 0 until sizeZ) {
            for (y in 0 until sizeY) {
                for (x in 0 until sizeX) {
                    val selfSolid = chunk.getBlock(x, y, z).isSolid
                    if (!selfSolid) continue
                    for (side in BlockSide.entries) {
                        val otherSolid = dimension // todo use own chunk for 99% of cases (much faster)
                            .getBlockAt(chunk.x0 + x + side.x, chunk.y0 + y + side.y, chunk.z0 + z + side.z)!!
                            .isSolid
                        if (!otherSolid) {
                            callback.call(x, y, z, side)
                        }
                    }
                }
            }
        }
    }

    val light = FloatArrayList(1 shl 16)
    val tmp = FloatArrayList(1 shl 16)

    fun lightTransport(
        sunDir: Vector3f,
        sunColors: List<Vector3f?>,
        skyColors: List<Vector3f>,
        numIterations: Int,
    ): FloatArrayList {
        light.ensureCapacity(faces.size * 3)
        light.size = faces.size * 3
        tmp.ensureCapacity(faces.size * 3)
        tmp.size = faces.size * 3
        light.fill(0f)

        addSkyEffect(skyColors)
        shadowCasting(sunDir, sunColors)

        for (i in 0 until numIterations) {
            val src = if (i.hasFlag(1)) tmp else light
            val dst = if (i.hasFlag(1)) light else tmp
            lightTransportI(src.values, dst.values)
        }
        return if (numIterations.hasFlag(1)) tmp else light
    }

    fun lightTransportI(src: FloatArray, dst: FloatArray) {
        src.copyInto(dst, 0, 0, 3 * faces.size)
        connections.forEach { srcI, dstI, r, g, b ->
            val srcI3 = srcI * 3
            val dstI3 = dstI * 3
            dst[dstI3 + 0] += src[srcI3 + 0] * r
            dst[dstI3 + 1] += src[srcI3 + 1] * g
            dst[dstI3 + 2] += src[srcI3 + 2] * b
        }
    }

    fun addSkyEffect(skyColors: List<Vector3f>) {
        val light = light.values
        val skyEffect = skyEffect.values
        faces.forEach { hash, faceId ->
            val f3 = faceId * 3
            val sky = skyColors[decodeSideId(hash)]
            val skyWeight = skyEffect[faceId]
            light[f3 + 0] += sky.x * skyWeight
            light[f3 + 1] += sky.y * skyWeight
            light[f3 + 2] += sky.z * skyWeight
        }
    }

    fun shadowCasting(dir: Vector3f, sunColors: List<Vector3f?>) {
        val query = query
        query.direction.set(dir)

        val maxDistance = 32.0
        val maxDistanceI = maxDistance.toInt()
        val maxSteps = ceil(maxDistance * 3).toInt()
        val bounds = AABBi().all()

        var receiveLight = 0
        val numSunRays = 100
        val light = light.values
        val random = Random(1234)
        faces.forEach { hash, faceId ->
            val x = decodeX(hash)
            val y = decodeY(hash)
            val z = decodeZ(hash)
            val sideId = decodeSideId(hash)
            val sunColor = sunColors[sideId]
            if (sunColor != null) {
                var numSunHits = 0
                val side = decodeSide(hash)
                repeat(numSunRays) {
                    // todo why do so few rays hit nothing???
                    val du = random.nextFloat() - 0.5f
                    val dv = random.nextFloat() - 0.5f
                    val posX = getPosX(x, side, du, dv) + side.x * 0.1f
                    val posY = getPosY(y, side, du, dv) + side.y * 0.1f
                    val posZ = getPosZ(z, side, du, dv) + side.z * 0.1f

                    query.start.set(posX, posY, posZ)
                    query.end.set(query.start).fma(maxDistance, dir)
                    query.result.distance = maxDistance

                    bounds
                        .setMin(x - maxDistanceI, y - maxDistanceI, z - maxDistanceI)
                        .setMax(x + maxDistanceI, y + maxDistanceI, z + maxDistanceI)

                    val sunIsBlocked = BlockTracing.blockTrace(query, maxSteps, bounds) { xi, yi, zi ->
                        val hit = dimension.getBlockAt(xi, yi, zi)!!.isSolid
                        if (hit) BlockTracing.SOLID_BLOCK
                        else BlockTracing.AIR_BLOCK
                    }
                    if (!sunIsBlocked) numSunHits++
                }

                if (numSunHits > 0) {
                    val weight = numSunHits.toFloat() / numSunRays.toFloat()
                    val f3 = faceId * 3
                    light[f3 + 0] += sunColor.x * weight
                    light[f3 + 1] += sunColor.y * weight
                    light[f3 + 2] += sunColor.z * weight
                    receiveLight++
                }
            }
        }

        // todo why do only soo few faces receive light???
        println("Receive light: $receiveLight / ${faces.size}")
    }

    fun getPosX(x: Int, side: BlockSide, du: Float, dv: Float): Double {
        return x + 0.5 * (side.x + 1) + du * side.y + dv * side.z
    }

    fun getPosY(y: Int, side: BlockSide, du: Float, dv: Float): Double {
        return y + 0.5 * (side.y + 1) + du * side.z + dv * side.x
    }

    fun getPosZ(z: Int, side: BlockSide, du: Float, dv: Float): Double {
        return z + 0.5 * (side.z + 1) + du * side.x + dv * side.y
    }
}
