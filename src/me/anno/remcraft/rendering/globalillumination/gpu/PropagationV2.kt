package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector3i

val propagationShaderV2 = ComputeShader(
    "propagation-v2", Vector3i(256, 1, 1),
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
        Variable(GLSLType.V3F, "sunDir"),
        Variable(GLSLType.V3F, "sunLight", 6),
    ), "" +

            randomness +
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

            "   float numSunHits = 0.0;\n" +
            "   vec3 sunLightI = sunLight[selfFace.w];\n" +
            "   bool canReceiveSunLight = dot(selfSide, sunDir) > 0.0 && sunLightI.x+sunLightI.y+sunLightI.z > 0.0;\n" + // early-out for back faces

            "   for (int i=0; i<raysPerFace; i++) {\n" +
            propagationCore +
            "       if (canReceiveSunLight) {\n" +
            "           numSunHits += 1.0 - blockTracing(pos, sunDir, hitDistance, hitFace, hitChunkData);\n" +
            "       }\n" +
            "   }\n" +
            sunApplyCode +
            "}\n"
)

val decayShader = ComputeShader(
    "decay", Vector3i(256, 1, 1),
    listOf(
        // we need atomic-add for floats, again...
        //  -> sun = max-value / 2?
        Variable(GLSLType.BUFFER, "lightBuffer")
            .defineBufferFormat("uvec4[] light;")
            .binding(0),
        Variable(GLSLType.V1F, "decayFactor"),
        Variable(GLSLType.V1I, "numFaces"),
    ), "" +
            "void main() {\n" +
            "   int selfFaceId = int(gl_GlobalInvocationID.x);\n" +
            "   if (selfFaceId >= numFaces) return;\n" +

            "   light[selfFaceId] = uvec4(light[selfFaceId] * decayFactor);\n" +
            "}\n"
)
