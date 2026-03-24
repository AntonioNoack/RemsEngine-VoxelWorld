package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.maths.Packing.pack32
import me.anno.maths.Packing.unpackHighFrom32
import me.anno.maths.Packing.unpackLowFrom32
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockColor.NUM_TEX_X
import me.anno.remcraft.block.BlockColor.NUM_TEX_Y
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.rendering.globalillumination.ChunkFaces.forEachFace
import me.anno.remcraft.rendering.globalillumination.GITextureShader
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosZ
import me.anno.remcraft.rendering.globalillumination.createWorld
import me.anno.remcraft.world.Dimension
import me.anno.remcraft.world.Index.totalSize
import me.anno.utils.OS.res
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.arrays.ShortArrayList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import speiger.primitivecollections.IntToIntHashMap
import kotlin.math.max
import kotlin.random.Random

// done:
//  1. read lighting from faces-list, via face-lookup
//  2. update lighting during runtime -> meh...

// todo shader to blur emission with neighboring faces (except sunLight)

// todo
//  3. allocate chunks in AllocationManager
//  4. chunk loader
//  5. combine with normal chunk loader

// todo flickering is annoying...
//  make it stable somehow...

fun main() {

    // todo number of iterations is not just a more-stable result, but gets blown out
    //  -> trace sun-rays every iteration???
    val simpleWorld = false
    val testDirectLighting = true
    val lightDecay = 0.98f
    val baseBrightness = 100f

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
    var numFrameIndex = 0

    val random = Random(15456)
    val sunLightBuffer = BlockSide.entries.flatMap { side ->
        val dir = sunDir.dot(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
        val brightness = max(baseBrightness * dir, 0f)
        listOf(
            sunColor.x * brightness,
            sunColor.y * brightness,
            sunColor.z * brightness
        )
    }.toFloatArray()

    val skyColor = Vector3f(66f, 67f, 68f)
        .mul(baseBrightness / 1000f)

    fun compute(numIterations: Int) {

        // todo multiply skyLight and sunLight by some factor at the start...
        val frameId = ++numFrameIndex

        val shader = propagationShaderV2
        shader.use()
        shader.v1i("chunkMap", chunkMapI)
        shader.v1i("numFaces", numFaces)
        shader.v1i("maxSteps", 300)
        shader.v3i("skyLight", skyColor.x.toInt(), skyColor.y.toInt(), skyColor.z.toInt())
        shader.v1i("raysPerFace", 100)
        shader.v3f("sunDir", sunDir)
        shader.v3fs("sunLight", sunLightBuffer)

        // bind all buffers
        shader.bindBuffer(2, data.chunkBuffer)
        shader.bindBuffer(3, data.faceBuffer)

        repeat(numIterations) {
            data.srcBuffer.copyElementsTo(data.dstBuffer, 0L, 0L, numFaces.toLong())

            shader.use()
            shader.v1i("baseSeed", random.nextInt())
            shader.bindBuffer(0, data.srcBuffer)
            shader.bindBuffer(1, data.dstBuffer)
            shader.runBySize(numFaces)
            GFX.check()

            glMemoryBarrier(BARRIER_BITS)
            GFX.check()

            // todo we can save lots of decays... we only need it at the ned...

            val shader1 = decayShader
            shader1.use()
            shader1.v1f("decayFactor", lightDecay)
            shader1.v1i("numFaces", numFaces)
            shader1.bindBuffer(0, data.srcBuffer)
            shader1.runBySize(numFaces)
            GFX.check()

            glMemoryBarrier(BARRIER_BITS)
            GFX.check()

            data.swap()
        }

        ShaderPrinting.printFromBuffer()
        GFX.check()
    }

    val scene = Entity()
        .add(MeshComponent(flatCube))
        .add(object : Component(), OnUpdate {
            var needsInit = true
            override fun onUpdate() {
                if (needsInit) {
                    needsInit = false
                    compute(10)
                    val newMesh = createDebugMesh(dimension, faceData)
                    if (testDirectLighting) {
                        val material = MaterialCache[newMesh.materials[0]]!!
                        val shader = GIDirectSunTextureShaderV2
                        shader.sunDir.set(sunDir)
                        shader.sunColor.set(sunColor).mul(7f)
                        shader.chunkMap = chunkMapI
                        shader.chunkBuffer = data.chunkBuffer
                        shader.faceBuffer = data.srcBuffer
                        material.shader = shader
                    }
                    getComponent(MeshComponent::class)!!.meshFile = newMesh.ref
                }/* else {
                    compute(1)
                }*/
            }
        })
    testSceneWithUI("Scene", scene)
}

fun ShortArrayList.add(x: Short, y: Short, z: Short, w: Short) {
    add(x)
    add(y)
    add(z)
    add(w)
}

fun createDebugMesh(
    dimension: Dimension,
    faces: IntArrayList,
): Mesh {
    // render chunk
    val mesh = Mesh()
    val positions = FloatArrayList()
    val normals = FloatArrayList()
    val faceIds = ShortArrayList(faces.size)
    val uvs = FloatArrayList()

    // this already works pretty well...
    //  -> special shader to map color into emissive
    //  and to load/show proper texture

    val flipSides = BooleanArray(6)
    flipSides[BlockSide.NX.ordinal] = true
    flipSides[BlockSide.NY.ordinal] = true
    flipSides[BlockSide.NZ.ordinal] = true

    for (i in faces.indices step 4) {
        val ki = faces[i]
        val kj = faces[i + 1]
        val x = unpackLowFrom32(ki, true)
        val y = unpackHighFrom32(ki, true)
        val z = unpackLowFrom32(kj, true)
        val sideId = unpackHighFrom32(kj, false)
        val side = BlockSide.entries[sideId]

        val block = dimension.getBlockAt(x, y, z) ?: BlockRegistry.Stone

        fun add(du: Float, dv: Float) {
            val px = getPosX(x, side, du, dv).toFloat()
            val py = getPosY(y, side, du, dv).toFloat()
            val pz = getPosZ(z, side, du, dv).toFloat()
            positions.add(px, py, pz)
            normals.add(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
            faceIds.add(x.toShort(), y.toShort(), z.toShort(), sideId.toShort())
            val u = du + 0.5f
            val v = dv + 0.5f
            uvs.add((u + block.texX) / NUM_TEX_X, 1f - (v + block.texY) / NUM_TEX_Y)
        }

        val du = 0.5f
        val dv = if (flipSides[side.ordinal]) -0.5f else +0.5f

        add(-du, -dv)
        add(-du, +dv)
        add(+du, +dv)

        add(-du, -dv)
        add(+du, +dv)
        add(+du, -dv)
    }

    mesh.positions = positions.toFloatArray()
    mesh.normals = normals.toFloatArray()
    mesh.faceIds = faceIds.toShortArray()
    mesh.uvs = uvs.toFloatArray()

    mesh.materials = listOf(Material().apply {
        shader = GITextureShader
        linearFiltering = false
        diffuseMap = res.getChild("textures/blocks/Blocks.png")
    }.ref)

    return mesh
}

var Mesh.faceIds: ShortArray?
    get() = getAttr("faceIds", ShortArray::class)
    set(value) = setAttr("faceIds", value, faceIdType)

private val faceIdType = Attribute("faceIds", AttributeType.SINT16, 4)