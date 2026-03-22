package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
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
import me.anno.input.Input
import me.anno.jvm.HiddenOpenGLContext
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.rendering.globalillumination.ChunkFaces.forEachFace
import me.anno.remcraft.rendering.globalillumination.createWorld
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.getIndex
import me.anno.remcraft.world.Index.totalSize
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.*
import speiger.primitivecollections.IntToIntHashMap

// todo do block-tracing on the GPU,
//  with dynamically loaded chunks


// todo IntToIntHashMap, just as a shader
//  data: (keys, values, capacity)
//  -> skip key=0 by adding 1 to all keys / reserving one value

// chunk data:
// capacity, [blocks: id,r,g,b], [2x capacity]

// todo convert chunk into list of faces,
//  dense block lookup,
//  and face -> faceId lookup (local?)

// todo two levels of HashMap:
//  chunkId -> chunk (pointer),
//  x,y,z,side -> faceId

// todo shaders, that we need:
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

fun IntArrayList.add(x: Int, y: Int, z: Int, side: Int) {
    add(x, y, z)
    add(side)
}

val IntToIntHashMap.capacity get() = content.mask + 1

private const val BARRIER_BITS =
    GL_SHADER_STORAGE_BARRIER_BIT or
            GL_BUFFER_UPDATE_BARRIER_BIT or
            GL_ELEMENT_ARRAY_BARRIER_BIT

fun main() {

    LogManager.disableLoggers("CacheSection,Saveable,ExtensionManager")
    OfficialExtensions.initForTests()

    // this will contain all hashMaps and blocks
    val chunkData = IntArrayList()
    val chunkMap = IntToIntHashMap(-1)
    val faceData = IntArrayList()

    fun pushMap(map: IntToIntHashMap): Int {
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

    fun pushBlocks(chunk: Chunk) {
        val blocks = chunk.blocks
        check(blocks.size == totalSize)
        for (index in blocks.indices) {
            val block = blocks[index]
            if (block == 0.toShort()) {
                chunkData.add(0)
            } else {
                val color = chunk.getBlock(index).color
                chunkData.add(color or 0xff000000.toInt())
            }
        }
    }

    var numFaces = 0
    createWorld(true) { chunk ->
        val faces = IntToIntHashMap(-1)
        chunk.forEachFace { x, y, z, side ->
            val hash = 1 + encodeSideLocal(x, y, z, side)
            faceData.add(chunk.x0 + x, chunk.y0 + y, chunk.z0 + z, side.ordinal)
            faces.put(hash, numFaces++)
        }

        val mapSize = 1 + faces.capacity * 2
        val blockSize = totalSize
        chunkData.ensureExtra(mapSize + blockSize)

        val entry = pushMap(faces)
        pushBlocks(chunk)

        chunkMap[HashChunkId(chunk.xi, chunk.yi, chunk.zi)] = entry
    }

    val chunkMapI = pushMap(chunkMap)

    // todo we don't have that many chunks, so we could store them in a dense map

    // create light buffers
    // todo initialize with some sun light?
    class GPUData {
        val chunkBuffer = chunkData.createBuffer("gi-chunks")
        val faceBuffer = faceData.createBuffer("gi-faces")
        val lightBuffer0 = createLightBuffer("gi-lightBuffer0", numFaces)
        val lightBuffer1 = createLightBuffer("gi-lightBuffer1", numFaces)
    }

    val data by lazy { GPUData() }

    fun compute() {
        val shader = propagationShader
        shader.use()
        shader.v1i("chunkMap", chunkMapI)
        shader.v1i("numFaces", numFaces)
        shader.v1i("maxSteps", 100)
        shader.v3i("skyLight", 66, 67, 68)

        // bind all buffers
        shader.bindBuffer(0, data.lightBuffer0)
        shader.bindBuffer(1, data.lightBuffer1)
        shader.bindBuffer(2, data.chunkBuffer)
        shader.bindBuffer(3, data.faceBuffer)

        shader.runBySize(numFaces)
        GFX.check()

        glMemoryBarrier(BARRIER_BITS)
        GFX.check()

        ShaderPrinting.printFromBuffer()
        GFX.check()

        // read back buffers for testing
        val lightValues = data.lightBuffer1.readAsIntArray()
        println(lightValues.toList())
    }

    if (true) {

        // todo continue debugging...

        HiddenOpenGLContext.createOpenGL()
        compute()

    } else {
        val scene = Entity()
            .add(MeshComponent(flatCube))
            .add(object : Component(), OnUpdate {
                override fun onUpdate() {
                    if (!Input.isKeyDown('x')) return
                    compute()
                }
            })
        testSceneWithUI("Scene", scene)
    }
}