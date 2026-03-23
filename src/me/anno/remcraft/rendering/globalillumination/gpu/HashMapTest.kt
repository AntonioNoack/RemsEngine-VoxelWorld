package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.engine.OfficialExtensions
import me.anno.gpu.GFX
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.gpu.shader.builder.Variable
import me.anno.jvm.HiddenOpenGLContext
import me.anno.remcraft.rendering.globalillumination.ChunkFaces.forEachFace
import me.anno.remcraft.rendering.globalillumination.createWorld
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3i
import speiger.primitivecollections.IntToIntHashMap

val hashMapTestShader = ComputeShader(
    "hashMapTest", Vector3i(256, 1, 1),
    listOf(
        Variable(GLSLType.BUFFER, "chunkBuffer")
            .defineBufferFormat("int[] ChunkData;")
            .binding(0),
        Variable(GLSLType.BUFFER, "queryBuffer")
            .defineBufferFormat("int[] QueryData;")
            .binding(1),
        Variable(GLSLType.V1I, "numQueries"),
    ), "" +

            ShaderPrinting.PRINTING_LIB +
            ShaderPrinting.definePrintCall(GLSLType.V1I, GLSLType.V1I, GLSLType.V1I, GLSLType.V1I) +

            hashMap +

            "void main() {\n" +
            "   int queryId = int(gl_GlobalInvocationID.x);\n" +
            "   if (queryId >= numQueries) return;\n" +

            "   int data  = QueryData[queryId*3];\n" +
            "   int key   = QueryData[queryId*3+1];\n" +
            "   int value = QueryData[queryId*3+2];\n" +
            "   HashMap map = HashMap(ChunkData[data], data + 1);\n" +
            "   int trueValue = HashMapGet(map,key);\n" +
            "   if (value != trueValue) {\n" +
            "       println(\"Mismatch: %d[%d] was %d, expected %d\", data, key, trueValue, value);\n" +
            "   }\n" +
            "}\n"
)

/**
 * results look as if some chunks cannot be traced properly... hash-mismatch?
 *  -> bruteforce checking all hashes, then validate
 *  -> chunkId function had collisions and 0 values (not supported on GPU)
 * */
fun main() {

    val simpleWorld = true

    LogManager.disableLoggers("CacheSection,Saveable,ExtensionManager")
    OfficialExtensions.initForTests()

    // this will contain all hashMaps and blocks
    val chunkData = IntArrayList()
    val chunkMap = IntToIntHashMap(0)
    val queryData = IntArrayList()
    val chunkKeys = IntArrayList()

    var numFaces = 0
    createWorld(simpleWorld) { chunk ->

        val faceMap = IntToIntHashMap(0)
        chunk.forEachFace { x, y, z, side ->
            val faceHash = encodeSideLocal(x, y, z, side)
            val faceId = numFaces++
            faceMap.put(faceHash, faceId)

            queryData.add(chunkData.size)
            queryData.add(faceHash)
            queryData.add(faceId)
        }

        val entry = pushMap(chunkData, faceMap)
        val chunkHash = hashChunkId(chunk.xi, chunk.yi, chunk.zi)
        chunkMap[chunkHash] = entry
        chunkKeys.add(chunkHash)
        chunkKeys.add(entry)
        println("ChunkMap[$chunkHash] = $entry")
    }

    val chunkMapI = pushMap(chunkData, chunkMap)
    println("ChunkMap: $chunkMapI")

    for (i in chunkKeys.indices step 2) {
        queryData.add(chunkMapI) // map
        queryData.add(chunkKeys[i]) // key
        queryData.add(chunkKeys[i + 1]) // value
    }

    HiddenOpenGLContext.createOpenGL()

    val chunkBuffer = chunkData.createBuffer("gi-chunks")
    val queryBuffer = queryData.createBuffer("gi-query")

    val shader = hashMapTestShader
    shader.use()

    // bind all buffers
    shader.bindBuffer(0, chunkBuffer)
    shader.bindBuffer(1, queryBuffer)
    shader.v1i("numQueries", queryData.size / 3)

    shader.runBySize(queryData.size / 3)
    GFX.check()

    ShaderPrinting.printFromBuffer()
    GFX.check()

}
