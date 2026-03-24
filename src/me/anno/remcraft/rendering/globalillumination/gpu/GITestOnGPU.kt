package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.GPUBuffer
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.maths.Packing.pack32
import me.anno.maths.Packing.unpackHighFrom32
import me.anno.maths.Packing.unpackLowFrom32
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.rendering.globalillumination.ChunkFaces.forEachFace
import me.anno.remcraft.rendering.globalillumination.createDebugMesh
import me.anno.remcraft.rendering.globalillumination.createWorld
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Dimension
import me.anno.remcraft.world.Index.getIndex
import me.anno.remcraft.world.Index.totalSize
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.lwjgl.opengl.GL46C.*
import speiger.primitivecollections.IntToIntHashMap
import kotlin.math.max
import kotlin.random.Random

// todo do block-tracing on the GPU,
//  with dynamically loaded chunks

// IntToIntHashMap, just as a shader
//  data: (keys, values, capacity)
//  -> skip key=0 by adding 1 to all keys / reserving one value

// chunk data:
// capacity, [blocks: id,r,g,b], [2x capacity]

// convert chunk into list of faces,
//  dense block lookup,
//  and face -> faceId lookup (local?)

// two levels of HashMap:
//  chunkId -> chunk (pointer),
//  x,y,z,side -> faceId

// shaders, that we need:
//  add sun-light
//  raytracing/propagation
//  decay for sun-light/previous stage?

fun encodeSideLocal(x: Int, y: Int, z: Int, side: BlockSide): Int {
    return 1 + getIndex(x, y, z) + side.ordinal * totalSize
}

val intAttr = bind(Attribute("data", AttributeType.UINT32, 1))
val lightAttr = bind(Attribute("data", AttributeType.UINT32, 4))

fun IntArrayList.createBuffer(name: String): GPUBuffer {
    val buffer = ComputeBuffer(name, intAttr, size)
    val nio = buffer.getOrCreateNioBuffer()
    nio.asIntBuffer().put(values, 0, size)
    nio.position(size)
    buffer.ensureBuffer()
    return buffer
}

fun createLightBuffer(name: String, numFaces: Int): GPUBuffer {
    val buffer = ComputeBuffer(name, lightAttr, numFaces)
    buffer.uploadEmpty(numFaces * 4 * 4L)
    buffer.zeroElements(0, numFaces)
    return buffer
}

fun IntArrayList.add(xy: Int, zs: Int, color: Int, unused: Int) {
    add(xy, zs, color)
    add(unused)
}

val IntToIntHashMap.capacity get() = content.mask + 1

const val BARRIER_BITS =
    GL_SHADER_STORAGE_BARRIER_BIT or
            GL_BUFFER_UPDATE_BARRIER_BIT or
            GL_ELEMENT_ARRAY_BARRIER_BIT

fun pushMap(chunkData: IntArrayList, map: IntToIntHashMap): Int {
    val offset = chunkData.size
    val capacity = map.capacity
    chunkData.ensureExtra(capacity * 2 + 1)
    chunkData.add(capacity)
    val keys = map.content.keys
    val values = map.content.values
    for (i in 0 until capacity) {
        chunkData.add(keys[i].toInt())
    }
    for (i in 0 until capacity) {
        chunkData.add(values[i].toInt())
    }
    return offset
}

fun pushBlocks(chunkData: IntArrayList, chunk: Chunk) {
    // encode bits into integer in 32-chunks (128kB -> 4kB)
    val blocks = chunk.blocks
    check(blocks.size == totalSize)
    val air = BlockRegistry.Air.id
    for (index in blocks.indices step 16) {
        var bits = 0
        for (di in 0 until 16) {
            val blockId = blocks[index + di]
            if (blockId != air) {
                val blockType = BlockRegistry.byId(blockId) ?: BlockRegistry.Stone
                val isSolid = blockType.isSolid && blockType != BlockRegistry.Leaves
                val type = if (isSolid) 2 else 1
                bits = bits or (type shl (di * 2))
            }
        }
        chunkData.add(bits)
    }
}

fun main() {

    // todo number of iterations is not just a more-stable result, but gets blown out
    //  -> trace sun-rays every iteration???
    val numIterations = 20
    val simpleWorld = false
    val interpolateColors = true
    val testDirectLighting = true

    LogManager.disableLoggers("CacheSection,Saveable,ExtensionManager")
    OfficialExtensions.initForTests()

    // this will contain all hashMaps and blocks
    val chunkData = IntArrayList()
    val chunkMap = IntToIntHashMap(0)
    val faceData = IntArrayList()

    var numFaces = 0
    val dimension = createWorld(simpleWorld) { chunk ->

        val faceMap = IntToIntHashMap(0)
        chunk.forEachFace { x, y, z, side ->
            val block = chunk.getBlock(x, y, z)
            val gx = chunk.x0 + x
            val gy = chunk.y0 + y
            val gz = chunk.z0 + z
            faceData.add(
                pack32(gy, gx),
                pack32(side.ordinal, gz),
                block.color, 0
            )
            val faceId = encodeSideLocal(x, y, z, side)
            faceMap.put(faceId, numFaces++)
        }

        val mapSize = 1 + faceMap.capacity * 2
        val blockSize = totalSize
        chunkData.ensureExtra(mapSize + blockSize)

        val entry = pushMap(chunkData, faceMap)
        pushBlocks(chunkData, chunk)

        val chunkHash = hashChunkId(chunk.xi, chunk.yi, chunk.zi)
        chunkMap[chunkHash] = entry
    }

    val chunkMapI = pushMap(chunkData, chunkMap)

    // todo we don't have that many chunks, so we could store them in a dense map

    // create light buffers
    class GPUData {
        val chunkBuffer = chunkData.createBuffer("gi-chunks")
        val faceBuffer = faceData.createBuffer("gi-faces")
        var srcBuffer = createLightBuffer("gi-lightBuffer0", numFaces)
        var dstBuffer = createLightBuffer("gi-lightBuffer1", numFaces)

        fun swap() {
            val tmp = srcBuffer
            srcBuffer = dstBuffer
            dstBuffer = tmp
        }
    }

    val data by lazy { GPUData() }

    val defaultSky = Skybox()
    val sunDir = Vector3f(defaultSky.sunBaseDir)
        .rotate(defaultSky.sunRotation)
        .normalize()

    val sunColor = Vector3f(1f, 1f, 0.9f)

    val random = Random(15456)
    fun computeSunLight() {

        val shader = sunLightShader
        shader.use()
        shader.v1i("chunkMap", chunkMapI)
        shader.v1i("numFaces", numFaces)
        shader.v1i("maxSteps", 1000)
        shader.v1i("baseSeed", random.nextInt())
        shader.v3f("sunDir", sunDir)
        shader.v3fs("sunLight", BlockSide.entries.flatMap { side ->
            val dir = sunDir.dot(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
            val brightness = max(1000f * dir, 0f)
            listOf(
                sunColor.x * brightness,
                sunColor.y * brightness,
                sunColor.z * brightness
            )
        }.toFloatArray())
        shader.v1i("raysPerFace", 100)

        // bind all buffers
        shader.bindBuffer(2, data.chunkBuffer)
        shader.bindBuffer(3, data.faceBuffer)
        shader.bindBuffer(1, data.dstBuffer)

        shader.runBySize(numFaces)
        GFX.check()

        glMemoryBarrier(BARRIER_BITS)
        GFX.check()

        data.swap()

        ShaderPrinting.printFromBuffer()
        GFX.check()

    }

    fun compute(): IntArray {

        computeSunLight()

        val justSunLight = if (testDirectLighting) {
            data.srcBuffer.readAsIntArray()
        } else IntArray(0)

        val shader = propagationShader
        shader.use()
        shader.v1i("chunkMap", chunkMapI)
        shader.v1i("numFaces", numFaces)
        shader.v1i("maxSteps", 1000)
        shader.v3i("skyLight", 660 / numIterations, 670 / numIterations, 680 / numIterations)
        shader.v1i("raysPerFace", 100)

        // bind all buffers
        shader.bindBuffer(2, data.chunkBuffer)
        shader.bindBuffer(3, data.faceBuffer)

        repeat(numIterations) {
            data.srcBuffer.copyElementsTo(data.dstBuffer, 0L, 0L, numFaces.toLong())

            shader.v1i("baseSeed", random.nextInt())
            shader.bindBuffer(0, data.srcBuffer)
            shader.bindBuffer(1, data.dstBuffer)
            shader.runBySize(numFaces)
            GFX.check()

            glMemoryBarrier(BARRIER_BITS)
            GFX.check()

            data.swap()
        }

        ShaderPrinting.printFromBuffer()
        GFX.check()

        // read back buffers for testing
        val allLight = data.srcBuffer.readAsIntArray()
        if (testDirectLighting) {
            for (i in allLight.indices) {
                allLight[i] -= justSunLight[i]
            }
        }
        return allLight
    }

    val scene = Entity()
        .add(MeshComponent(flatCube))
        .add(object : Component(), OnUpdate {
            var needsInit = true
            override fun onUpdate() {
                if (needsInit) {
                    needsInit = false
                    val data1 = compute()
                    println(data1.toList().take(100))
                    val newMesh = createDebugMesh(dimension, faceData, data1, sunColor, interpolateColors)
                    if (testDirectLighting) {
                        val material = MaterialCache[newMesh.materials[0]]!!
                        val shader = GIDirectSunTextureShader
                        shader.sunDir.set(sunDir)
                        shader.sunColor.set(sunColor).mul(7f)
                        shader.chunkMap = chunkMapI
                        shader.chunkBuffer = data.chunkBuffer
                        material.shader = shader
                    }
                    getComponent(MeshComponent::class)!!.meshFile = newMesh.ref
                }
            }
        })
    testSceneWithUI("Scene", scene)
}

fun createDebugMesh(
    dimension: Dimension,
    faces: IntArrayList,
    light: IntArray, sunColor: Vector3f,
    interpolateColors: Boolean,
): Mesh {
    val lightScale = 5f / light.max()
    val f = 1f / sunColor.max()
    val fr = sunColor.x * f
    val fg = sunColor.y * f
    val fb = sunColor.z * f
    return createDebugMesh(dimension, interpolateColors) { callback ->
        for (i in faces.indices step 4) {
            val faceId = i shr 2
            val ki = faces[i]
            val kj = faces[i + 1]
            val x = unpackLowFrom32(ki, true)
            val y = unpackHighFrom32(ki, true)
            val z = unpackLowFrom32(kj, true)
            val sideId = unpackHighFrom32(kj, false)
            val side = BlockSide.entries[sideId]

            val directSunLight = light[faceId * 4 + 3]
            val cr = (light[faceId * 4 + 0] - directSunLight * fr) * lightScale
            val cg = (light[faceId * 4 + 1] - directSunLight * fg) * lightScale
            val cb = (light[faceId * 4 + 2] - directSunLight * fb) * lightScale

            callback(x, y, z, side, cr, cg, cb)
        }
    }
}
