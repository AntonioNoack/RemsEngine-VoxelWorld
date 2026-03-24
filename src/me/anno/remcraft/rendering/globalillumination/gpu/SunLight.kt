package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector3i

val sunApplyCode =
    "" +
            "   if (numSunHits > 0.0) {\n" +
            "       float sunBrightness = max(sunLightI.r,max(sunLightI.g,sunLightI.b));\n" +
            "       atomicAdd(dstLight[selfFaceId].r, uint(numSunHits * sunLightI.r));\n" +
            "       atomicAdd(dstLight[selfFaceId].g, uint(numSunHits * sunLightI.g));\n" +
            "       atomicAdd(dstLight[selfFaceId].b, uint(numSunHits * sunLightI.b));\n" +
            "       atomicAdd(dstLight[selfFaceId].a, uint(numSunHits * sunBrightness));\n" +
            "   }\n"

val sunLightShader = ComputeShader(
    "sunLight", Vector3i(256, 1, 1),
    listOf(
        // we need atomic-add for floats, again...
        //  -> sun = max-value / 2?
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
        Variable(GLSLType.V1I, "chunkMap"),
        Variable(GLSLType.V1I, "maxSteps"),
        Variable(GLSLType.V1I, "baseSeed"),
        Variable(GLSLType.V1I, "raysPerFace"),
        Variable(GLSLType.V3F, "sunDir"),
        Variable(GLSLType.V3F, "sunLight", 6),
    ), "" +

            ShaderPrinting.PRINTING_LIB +
            ShaderPrinting.definePrintCall(GLSLType.V1F, GLSLType.V1F, GLSLType.V3F) +

            randomness +
            hashMap +
            blockTracing +
            getSide +
            loadFace +
            atomicAddColor +

            "void main() {\n" +
            "   int selfFaceId = int(gl_GlobalInvocationID.x);\n" +
            "   if (selfFaceId >= numFaces) return;\n" +

            "   vec3 selfColor = vec3(0);\n" +
            "   ivec4 selfFace = loadFace(FaceData[selfFaceId], selfColor);\n" +
            "   vec3 selfSide = getSide(selfFace.w);\n" +
            "   vec3 sunLightI = sunLight[selfFace.w];\n" +
            "   if (dot(selfSide, sunDir) <= 0.0 || sunLightI.x+sunLightI.y+sunLightI.z <= 0.0) return;\n" + // early-out for back faces

            "   float numSunHits = 0.0;\n" +
            "   HashMap chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);\n" +
            "   uint seed = createSeed(uint(baseSeed), uint(selfFaceId), 1u);\n" +
            "   for (int i=0; i<raysPerFace; i++) {\n" +
            // sample randomly, [-0.5, +0.5]
            "       float du = nextFloat(seed)-0.5;\n" +
            "       float dv = nextFloat(seed)-0.5;\n" +
            "       vec3 pos = getPos(selfFace,du,dv) + 0.01 * selfSide;\n" +

            "       float hitDistance = 0.0;\n" +
            "       ivec4 hitFace = ivec4(0);\n" +
            "       int hitChunkData = 0;\n" +
            "       numSunHits += 1.0 - blockTracing(pos, sunDir, hitDistance, hitFace, hitChunkData);\n" +
            "   }\n" +
            sunApplyCode +
            "}\n"
)
