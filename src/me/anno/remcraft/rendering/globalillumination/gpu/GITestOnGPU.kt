package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.engine.OfficialExtensions
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.GPUBuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.jvm.HiddenOpenGLContext
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.rendering.globalillumination.ChunkFaces.forEachFace
import me.anno.remcraft.rendering.globalillumination.createWorld
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsY
import me.anno.remcraft.world.Index.bitsZ
import me.anno.remcraft.world.Index.getIndex
import me.anno.remcraft.world.Index.maskX
import me.anno.remcraft.world.Index.maskY
import me.anno.remcraft.world.Index.maskZ
import me.anno.remcraft.world.Index.totalSize
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BARRIER_BIT
import speiger.primitivecollections.IntToIntHashMap

// todo do block-tracing on the GPU,
//  with dynamically loaded chunks


// todo IntToIntHashMap, just as a shader
//  data: (keys, values, capacity)
//  -> skip key=0 by adding 1 to all keys / reserving one value

val gpuHashMap = """
        struct HashMap {
            int capacity;
            int entryOffset;
        };
        int HashUtilMix(int x) {
            uint h = uint(x * -1640531527);
            return int(h ^ (h >> 16));
        }
        int HashMapGetKey(HashMap hashMap, int pos) {
            return ChunkData[hashMap.entryOffset + pos];
        }
        int HashMapGetValue(HashMap hashMap, int pos) {
            return ChunkData[hashMap.entryOffset + hashMap.capacity + pos];
        }
        int HashMapGet(HashMap hashMap, int key) {
            int mask = hashMap.capacity - 1;
            int pos = HashUtilMix(key) & mask;
            while (true) {
                int current = HashMapGetKey(hashMap, pos);
                if (current == 0) return -1;
                if (current == key) return HashMapGetValue(hashMap, pos);
                pos = (pos + 1) & mask;
            }
        }
    """.trimIndent()

// chunk data:
// capacity, [blocks: id,r,g,b], [2x capacity]

fun hashChunkId(xi: Int, yi: Int, zi: Int): Int {
    val mask = 0x7fff_ffff
    return xi xor yi.and(mask).shl(11) xor zi.and(mask).shl(22)
}

val blockTracing = """
        int HashChunkId(uvec3 chunkId) {
            return int(chunkId.x ^ (chunkId.y << 11) ^ (chunkId.z << 22));
        }
        int GetBlockInChunk(int chunkData, ivec3 blockPos) {
            blockPos = blockPos & ivec3($maskX,$maskY,$maskZ);
            int blockId = 1 + blockPos.x + (blockPos.y << $bitsX) + (blockPos.z << ${bitsX + bitsY});
            return ChunkData[chunkData + blockId];
        }
        bool blockTracing(
            vec3 localStart, vec3 dir,
            out float hitDistance,
            out ivec4 hitFace,
            out int hitBlockId
        ) {
            if (abs(dir.x) < 1e-7) dir.x = 1e-7;
            if (abs(dir.y) < 1e-7) dir.y = 1e-7;
            if (abs(dir.z) < 1e-7) dir.z = 1e-7;
            vec3 dirSign = sign(dir);
            ivec3 blockPos = ivec3(floor(localStart));
            vec3 dist3 = (dirSign*.5+.5 + vec3(blockPos) - localStart) / dir;
            vec3 invUStep = dirSign / dir;
            float nextDist, dist = 0.0;
            ivec3 faceCandidates = ivec3(
                dir.x < 0.0 ? ${BlockSide.NX.ordinal} : ${BlockSide.PX.ordinal},
                dir.y < 0.0 ? ${BlockSide.NY.ordinal} : ${BlockSide.PY.ordinal},
                dir.z < 0.0 ? ${BlockSide.NZ.ordinal} : ${BlockSide.PZ.ordinal}
            );
            
            vec3 dtf3 = localStart / dir;
            float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));
            int faceId = dtf3.z == dtf1 ? faceCandidates.z
                       : dtf3.y == dtf1 ? faceCandidates.y
                       :                  faceCandidates.x;
                       
            uvec3 chunkId = ivec3(0xffffffff);
            int chunkData = -1;
            
            hitBlockId = 0;
            hitFace = ivec4(0);
            hitDistance = -1.0;
            
            HashMap chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);
            
            ivec3 dirSignI = ivec3(dirSign);
            for (int i=0; i<maxSteps; i++) {
                nextDist = min(dist3.x, min(dist3.y, dist3.z));
                bool continueTracing = true;
                float skippingDist = 0.0;
                
                // check block is here:
                uvec3 newChunkId = uvec3(blockPos) >> ivec3($bitsX,$bitsY,$bitsZ);
                if (newChunkId != chunkId) {
                    // if chunkId has changed, change chunk
                    chunkData = HashMapGet(chunkHashMap, HashChunkId(newChunkId));
                    chunkId = newChunkId;
                }
                
                if (chunkData != -1) {
                    hitBlockId = GetBlockInChunk(chunkData, blockPos);
                    continueTracing = hitBlockId == 0;
                }
                
                if (skippingDist >= 1.0) {
                    // todo if a chunk is missing, skip it quickly...
                   vec3 blockPosF = floor(localStart + dir * (dist + skippingDist));
                   blockPos = ivec3(blockPosF);
                   dist3 = (dirSign*.5+.5 + blockPosF - localStart)/dir;
                   nextDist = min(dist3.x, min(dist3.y, dist3.z));
                   dist = nextDist;
               } else {
                   if (nextDist == dist3.x) {
                       blockPos.x += dirSignI.x;
                       dist3.x += invUStep.x;
                       faceId = faceCandidates.x;
                   } else if (nextDist == dist3.y) {
                       blockPos.y += dirSignI.y;
                       dist3.y += invUStep.y;
                       faceId = faceCandidates.y;
                   } else {
                       blockPos.z += dirSignI.z;
                       dist3.z += invUStep.z;
                       faceId = faceCandidates.z;
                   }
                   dist = nextDist;
               }
            }
            if (hitBlockId == 0) return false;
            
            vec3 localPos = localStart + dir * dist;
            
            hitDistance = dist;
            hitFace = ivec4(blockPos, faceId);
            
            return true;
        }
    """.trimIndent()

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

val propagationShader = ComputeShader(
    "propagation", Vector3i(256, 1, 1),
    listOf(
        // we need atomic-add for floats, again...
        //  -> sun = max-value / 2?
        Variable(GLSLType.BUFFER, "srcBuffer")
            .defineBufferFormat("uvec4[] srcLight;")
            .binding(0),
        Variable(GLSLType.BUFFER, "dstBuffer")
            .defineBufferFormat("uvec4[] dstLight;")
            .binding(1),
        Variable(GLSLType.BUFFER, "chunkBuffer")
            .defineBufferFormat("int[] ChunkData;")
            .binding(2),
        Variable(GLSLType.BUFFER, "faceBuffer")
            .defineBufferFormat("ivec4[] FaceData;")
            .binding(3),
        Variable(GLSLType.V1I, "numFaces"),
        Variable(GLSLType.V4I, "skyLight"),
        Variable(GLSLType.V1I, "chunkMap"),
        Variable(GLSLType.V1I, "maxSteps"),
    ), "" +

            gpuHashMap +
            blockTracing +

            /*
        fun getPosX(x: Int, side: BlockSide, du: Float, dv: Float): Double
            return x + 0.5 * (side.x + 1) + du * side.y + dv * side.z
        fun getPosY(y: Int, side: BlockSide, du: Float, dv: Float): Double
            return y + 0.5 * (side.y + 1) + du * side.z + dv * side.x
        fun getPosZ(z: Int, side: BlockSide, du: Float, dv: Float): Double
            return z + 0.5 * (side.z + 1) + du * side.x + dv * side.y
        }*/

            "vec3 getSide(int index) {\n" +
            "   switch (index) {\n" +
            BlockSide.entries.joinToString("\n") { side ->
                "case ${side.ordinal}: return vec3(${side.x},${side.y},${side.z});\n"
            } +
            "       default: return vec3(0);\n" +
            "   }\n" +
            "}\n" +

            "ivec3 getSideI(int index) {\n" +
            "   switch (index) {\n" +
            BlockSide.entries.joinToString("\n") { side ->
                "case ${side.ordinal}: return ivec3(${side.x},${side.y},${side.z});\n"
            } +
            "       default: return ivec3(0);\n" +
            "   }\n" +
            "}\n" +

            "vec3 getPos(ivec4 faceId, float du, float dv) {\n" +
            "   vec3 side = getSide(faceId.w);\n" +
            "   return vec3(faceId.xyz) + 0.5 * (side + 1.0) + du * side.yzx + dv * side.zxy;\n" +
            "}\n" +

            "void main() {\n" +
            "   int idx = int(gl_GlobalInvocationID.x);\n" +
            "   if (idx >= numFaces) return;\n" +

            // todo we also need a sun-phase, where we read the shadow-map, and apply sun-irradiance to all faces

            "   int numRays = 50;\n" +
            "   float baseWeight = 0.5 / float(numRays);\n" +
            "   for (int i=0; i<numRays; i++) {\n" +
            "       ivec4 selfFace = FaceData[i];\n" +
            // todo sample randomly, [-0.5, +0.5]
            "       float du = 0.0;\n" +
            "       float dv = 0.0;\n" +
            "       vec3 pos = getPos(selfFace,du,dv) + 0.1 * getSide(selfFace.w);\n" +
            // todo sample randomly, then normalize
            "       vec3 dir = getSide(selfFace.w);\n" +

            "       float hitDistance = 0.0;\n" +
            "       ivec4 hitFace = ivec4(0);\n" +
            "       int hitBlockId = 0;\n" +

            "       bool hitBlock = blockTracing(pos, dir, hitDistance, hitFace, hitBlockId);\n" +
            "       if (hitBlock) {\n" +
            "           float weight = baseWeight / (1.0 + hitDistance * hitDistance);\n" +
            // todo add cross-illumination:
            // todo find faceIndex of target
            "       } else {\n" +
            // add sky-contribution to dst
            // todo make side/direction-dependent
            "           atomicAdd(dstLight[idx].x, uint(skyLight.x));\n" +
            "           atomicAdd(dstLight[idx].y, uint(skyLight.y));\n" +
            "           atomicAdd(dstLight[idx].z, uint(skyLight.z));\n" +
            "       }\n" +
            "   }\n" +
            "}\n"
)

fun encodeSideLocal(x: Int, y: Int, z: Int, side: BlockSide): Int {
    return 1 + getIndex(x, y, z) + side.ordinal * totalSize
}

val intAttr = bind(Attribute("data", AttributeType.UINT32, 1))
val lightAttr = bind(Attribute("data", AttributeType.FLOAT, 4))

fun IntArrayList.createBuffer(): GPUBuffer {
    val buffer = ComputeBuffer("chunks", intAttr, size)
    val nio = buffer.getOrCreateNioBuffer()
    nio.asIntBuffer().put(values, 0, size)
    nio.position(size)
    buffer.ensureBuffer()
    return buffer
}

fun createLightBuffer(numFaces: Int): GPUBuffer {
    val buffer = ComputeBuffer("light", lightAttr, numFaces)
    buffer.uploadEmpty(numFaces * 4 * 4L)
    buffer.zeroElements(0, numFaces)
    return buffer
}

fun IntArrayList.add(x: Int, y: Int, z: Int, side: Int) {
    add(x)
    add(y)
    add(z)
    add(side)
}

val IntToIntHashMap.capacity get() = content.mask + 1

private const val BARRIER_BITS =
    GL_SHADER_STORAGE_BARRIER_BIT or
            GL_BUFFER_UPDATE_BARRIER_BIT or
            GL_ELEMENT_ARRAY_BARRIER_BIT

fun main() {

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
    createWorld(false) { chunk ->
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

        chunkMap[hashChunkId(chunk.xi, chunk.yi, chunk.zi)] = entry
    }

    val chunkMapI = pushMap(chunkMap)

    HiddenOpenGLContext.createOpenGL()

    val chunkBuffer = chunkData.createBuffer()
    val faceBuffer = faceData.createBuffer()
    val lightBuffer0 = createLightBuffer(numFaces)
    val lightBuffer1 = createLightBuffer(numFaces)

    // todo we don't have that many chunks, so we could store them in a dense map

    // create light buffers
    // todo initialize with some sun light?

    val shader = propagationShader
    shader.use()
    shader.v1i("chunkMap", chunkMapI)
    shader.v1i("numFaces", numFaces)
    shader.v1i("maxSteps", 100)

    // bind all buffers
    shader.bindBuffer(0, lightBuffer0)
    shader.bindBuffer(1, lightBuffer1)
    shader.bindBuffer(2, chunkBuffer)
    shader.bindBuffer(3, faceBuffer)

    shader.runBySize(numFaces)

    glMemoryBarrier(BARRIER_BITS)

    // todo read back buffers for testing

    // todo load set of chunks
    // todo allocator for hashmaps
    // todo fill chunks and chunk data into allocator
    // todo define block-lookup function...

}