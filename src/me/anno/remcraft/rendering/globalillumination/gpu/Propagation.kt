package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsZ
import me.anno.remcraft.world.Index.maskX
import me.anno.remcraft.world.Index.maskY
import me.anno.remcraft.world.Index.maskZ
import me.anno.remcraft.world.Index.totalSize
import org.joml.Vector3i

val getSide = "" +
        "vec3 getSide(int index) {\n" +
        "   switch (index) {\n" +
        BlockSide.entries.joinToString("") { side ->
            "case ${side.ordinal}: return vec3(${side.x},${side.y},${side.z});\n"
        } +
        "       default: return vec3(0);\n" +
        "   }\n" +
        "}\n" +

        "ivec3 getSideI(int index) {\n" +
        "   switch (index) {\n" +
        BlockSide.entries.joinToString("") { side ->
            "case ${side.ordinal}: return ivec3(${side.x},${side.y},${side.z});\n"
        } +
        "       default: return ivec3(0);\n" +
        "   }\n" +
        "}\n" +

        "vec3 getPos(ivec4 faceId, float du, float dv) {\n" +
        "   vec3 side = getSide(faceId.w);\n" +
        "   return vec3(faceId.xyz) + 0.5 * (side + 1.0) + du * side.yzx + dv * side.zxy;\n" +
        "}\n" +

        "int encodeSideLocal(ivec4 faceId) {\n" +
        "   ivec3 pos = faceId.xyz & ivec3($maskX,$maskY,$maskZ);\n" +
        "   return 1 + pos.x + (pos.y<<${bitsX + bitsZ}) + (pos.z<<$bitsX) + faceId.w * $totalSize;\n" +
        "}\n"

val loadFace = "" +
        // we need the following:
        //  x, y, z, faceId,
        //  diffuse color
        //  later: roughness, metallic, emissive
        "ivec4 loadFace(ivec4 rawData, out vec3 color) {\n" +
        "   int x = (rawData.x << 16) >> 16;\n" +
        "   int y = rawData.x >> 16;\n" +
        "   int z = (rawData.y << 16) >> 16;\n" +
        "   int side = rawData.y >> 16;\n" +
        "   int rgb = rawData.z;\n" +
        "   color = vec3((rgb>>16)&255, (rgb>>8)&255, rgb&255)/255.0;\n" +
        "   return ivec4(x,y,z,side);\n" +
        "}\n"

val propagationCore = "" +
        // sample randomly, [-0.5, +0.5]
        "       float du = nextFloat(seed)-0.5;\n" +
        "       float dv = nextFloat(seed)-0.5;\n" +
        "       vec3 pos = getPos(selfFace,du,dv) + 0.01 * selfSide;\n" +
        // sample randomly, then normalize
        "       vec3 dir = selfSide * 1.001;\n" +
        "       dir.x += nextFloat(seed)*2.0-1.0;\n" +
        "       dir.y += nextFloat(seed)*2.0-1.0;\n" +
        "       dir.z += nextFloat(seed)*2.0-1.0;\n" +
        "       dir = normalize(dir);\n" +

        //"       if(selfFaceId<10) println(\"Starting ray [%d,%d] -> [%d,%d,%d,%d] from (%f,%f,%f) += t * (%f,%f,%f)\", " +
        //"           selfFaceId, i, selfFace.x, selfFace.y, selfFace.z, selfFace.w, pos, dir);\n" +

        "       float hitDistance = 0.0;\n" +
        "       ivec4 hitFace = ivec4(0);\n" +
        "       int hitChunkData = 0;\n" +

        "       float opacity = blockTracing(pos, dir, hitDistance, hitFace, hitChunkData);\n" +
        "       if (opacity == 1.0) {\n" +
        "           float weight = baseWeight / (1.0 + hitDistance * hitDistance);\n" +
        // find faceIndex of target
        //  -> find face in chunk's buffer
        "           HashMap faceMap = HashMap(ChunkData[hitChunkData], hitChunkData+1);\n" +
        "           int otherFaceId = HashMapGet(faceMap, encodeSideLocal(hitFace));\n" +
        "           if (otherFaceId == -1) continue;\n" + // error, hit missing face :(
        // "           if (selfFaceId == 0) println(\"Found distance %f\", hitDistance);\n" +
        "           vec3 otherColor = vec3(0.0);\n" +
        "           loadFace(FaceData[otherFaceId], otherColor);\n" +
        // "           if (selfFaceId == 0) println(\"Colors self: (%f,%f,%f), other: (%f,%f,%f)\", selfColor, otherColor);\n" +
        // add cross-illumination:
        "           atomicAddColor(otherFaceId, weight *  selfColor * vec3(srcLight[ selfFaceId].rgb));\n" +
        "           atomicAddColor( selfFaceId, weight * otherColor * vec3(srcLight[otherFaceId].rgb));\n" +
        "       } else {\n" +
        // add sky-contribution to dst
        // todo make side/direction-dependent
        "           atomicAdd(dstLight[selfFaceId].x, uint(skyLight.x));\n" +
        "           atomicAdd(dstLight[selfFaceId].y, uint(skyLight.y));\n" +
        "           atomicAdd(dstLight[selfFaceId].z, uint(skyLight.z));\n" +
        "       }\n"


val atomicAddColor = "" +
        "void atomicAddColor(int idx, vec3 color) {\n" +
        "   uvec3 rgb = uvec3(color);\n" +
        "   atomicAdd(dstLight[idx].x, rgb.x);\n" +
        "   atomicAdd(dstLight[idx].y, rgb.y);\n" +
        "   atomicAdd(dstLight[idx].z, rgb.z);\n" +
        "}\n"

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
        Variable(GLSLType.V3I, "skyLight"),
        Variable(GLSLType.V1I, "chunkMap"),
        Variable(GLSLType.V1I, "maxSteps"),
        Variable(GLSLType.V1I, "baseSeed"),
        Variable(GLSLType.V1I, "raysPerFace"),
    ), randomness +
            hashMap +
            blockTracing +
            getSide +
            loadFace +
            atomicAddColor +

            "void main() {\n" +
            "   int selfFaceId = int(gl_GlobalInvocationID.x);\n" +
            "   if (selfFaceId >= numFaces) return;\n" +

            "   HashMap chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);\n" +
            "   float baseWeight = 0.1 / float(raysPerFace);\n" +
            "   uint seed = createSeed(uint(baseSeed), uint(selfFaceId), 1u);\n" +
            "   vec3 selfColor = vec3(0);\n" +
            "   ivec4 selfFace = loadFace(FaceData[selfFaceId], selfColor);\n" +
            "   vec3 selfSide = getSide(selfFace.w);\n" +
            "   for (int i=0; i<raysPerFace; i++) {\n" +
            propagationCore +
            "   }\n" +
            "}\n"
)
