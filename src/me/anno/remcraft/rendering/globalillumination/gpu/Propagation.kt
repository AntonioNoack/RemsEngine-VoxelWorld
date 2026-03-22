package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.gpu.shader.builder.Variable
import me.anno.mesh.vox.meshing.BlockSide
import org.joml.Vector3i

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
    ), "" +

            ShaderPrinting.PRINTING_LIB +
            ShaderPrinting.definePrintCall(listOf(
                GLSLType.V1I, GLSLType.V1I,
                GLSLType.V1I, GLSLType.V1I, GLSLType.V1I, GLSLType.V1I,
                GLSLType.V3F, GLSLType.V3F)) +

            gpuHashMap +
            blockTracing +

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

            "void main() {\n" +
            "   int idx = int(gl_GlobalInvocationID.x);\n" +
            "   if (idx >= numFaces) return;\n" +

            // todo we also need a sun-phase, where we read the shadow-map, and apply sun-irradiance to all faces

            "   int numRays = 1;\n" +
            "   float baseWeight = 0.5 / float(numRays);\n" +
            "   for (int i=0; i<numRays; i++) {\n" +
            "       ivec4 selfFace = FaceData[i];\n" +
            // todo sample randomly, [-0.5, +0.5]
            "       float du = 0.0;\n" +
            "       float dv = 0.0;\n" +
            "       vec3 pos = getPos(selfFace,du,dv) + 0.1 * getSide(selfFace.w);\n" +
            // todo sample randomly, then normalize
            "       vec3 dir = getSide(selfFace.w);\n" +

            "       if(idx<10) println(\"Starting ray [%d,%d] -> [%d,%d,%d,%d] from (%f,%f,%f) += t * (%f,%f,%f)\", " +
            "           idx, i, selfFace.x, selfFace.y, selfFace.z, selfFace.w, pos, dir);\n" +

            "       float hitDistance = 0.0;\n" +
            "       ivec4 hitFace = ivec4(0);\n" +
            "       int hitBlockId = 0;\n" +

            "       bool hitBlock = blockTracing(pos, dir, hitDistance, hitFace, hitBlockId);\n" +
            "       if (hitBlock) {\n" +
            "           float weight = baseWeight / (1.0 + hitDistance * hitDistance);\n" +
            // todo add cross-illumination:
            // todo find faceIndex of target
            //  -> load chunk of target
            //  -> find face in chunk's buffer
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
