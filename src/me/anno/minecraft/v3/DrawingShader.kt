package me.anno.minecraft.v3

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.minecraft.v3.CachedRendering.Companion.CHUNK_SIZE
import me.anno.minecraft.v3.CachedRendering.Companion.DELTA_Y
import me.anno.minecraft.v3.CachedRendering.Companion.getIndex
import me.anno.minecraft.v3.CachedRendering.Companion.getIndexXZ
import me.anno.minecraft.v3.CachedRendering.Companion.isOutOfBounds
import kotlin.math.max

object DrawingShader : Shader(
    "blockTracing", listOf(
        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
        Variable(GLSLType.M4x4, "transform"),
        Variable(GLSLType.M4x3, "localTransform"),
    ), "" +
            "void main() {\n" +
            "   vec3 pos = matMul(localTransform,vec4(coords,1.0));\n" +
            "   gl_Position = matMul(transform,vec4(pos,1.0));\n" +
            "   dir0 = pos;\n" +
            "}", listOf(
        Variable(GLSLType.V3F, "dir0")
    ), listOf(
        Variable(GLSLType.V3F, "cameraPosition"), // relative to 0,0,0 in the chunk (?)
        Variable(GLSLType.SCube, "resultTex"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT)
    ), "" +
            "layout(std430, binding = 0) readonly buffer blockBuffer { uint blocks[]; };\n" +
            getIndexXZ + getIndex +
            "void main() {\n" +
            "vec3 dir = normalize(dir0);\n" +
// discard, if block at that position is already colored (can save tons of time)
            "if(texture(resultTex,-dir*$cubemapsAreLeftHanded,0.0).a > 0.0) discard;\n" +
            "vec3 bounds0 = vec3($CHUNK_SIZE,$DELTA_Y,$CHUNK_SIZE), halfBounds = bounds0 * 0.5;\n" +
            "vec3 bounds1 = bounds0 - 1.0;\n" +
// prevent divisions by zero
            "if(abs(dir.x) < 1e-7) dir.x = 1e-7;\n" +
            "if(abs(dir.y) < 1e-7) dir.y = 1e-7;\n" +
            "if(abs(dir.z) < 1e-7) dir.z = 1e-7;\n" +
            "vec3 localStart = cameraPosition;\n" +
// start from camera, and project onto front sides
// for proper rendering, we need to use the backsides, and therefore we project the ray from the back onto the front
            "vec3 dirSign = sign(dir);\n" +
            "vec3 dtf3 = (dirSign * halfBounds + localStart) / dir;\n" +
            "float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));\n" +
            "float dtf = min(dtf1, 0.0);\n" +
            "localStart += -dtf * dir + halfBounds;\n" +
            "vec3 blockPosition = floor(localStart);\n" +
            "vec3 dist3 = (dirSign*.5+.5 + blockPosition - localStart)/dir;\n" +
            "vec3 invUStep = dirSign / dir;\n" +
            "float nextDist, dist = 0.0;\n" +
            "int lastNormal = dtf3.z == dtf1 ? 2 : dtf3.y == dtf1 ? 1 : 0, i;\n" +
            "int maxSteps = ${CHUNK_SIZE * 2 + DELTA_Y};\n" +
            "uint hitSomething = 0u;\n" +
            "for(i=0;i<maxSteps;i++){\n" +
            "   nextDist = min(dist3.x, min(dist3.y, dist3.z));\n" +
            "   bool isOutOfBounds = $isOutOfBounds;\n" +
            "   if (isOutOfBounds && i>0) break;\n" +
            "   uint index = getIndex(getXZIndex(ivec2(blockPosition.xz)), uint(blockPosition.y));\n" +
            // "   hitSomething = isOutOfBounds ? 0u : int(dot(blockPosition,vec3(1))) % 20 < 10 ? 0xffc900u : 0u;//blocks[index];\n" +
            "   hitSomething = isOutOfBounds ? 0u : blocks[index];\n" +
            "   if (hitSomething == 0u){\n" + // continue traversal
            "       if (nextDist == dist3.x){\n" +
            "           blockPosition.x += dirSign.x; dist3.x += invUStep.x; lastNormal = 0;\n" +
            "           if(blockPosition.x < 0.0 || blockPosition.x > bounds1.x){ break; }\n" +
            "       } else if (nextDist == dist3.y){\n" +
            "           blockPosition.y += dirSign.y; dist3.y += invUStep.y; lastNormal = 1;\n" +
            "           if(blockPosition.y < 0.0 || blockPosition.y > bounds1.y){ break; }\n" +
            "       } else {\n" +
            "           blockPosition.z += dirSign.z; dist3.z += invUStep.z; lastNormal = 2;\n" +
            "           if(blockPosition.z < 0.0 || blockPosition.z > bounds1.z){ break; }\n" +
            "       }\n" +
            "       dist = nextDist;\n" +
            "   } else { break; }\n" + // hit something :)
            "}\n" +
// compute normal
            "int normalIdx = 0;\n" +
            "if (hitSomething != 0u) {\n" +
            "   if (lastNormal == 0) { normalIdx = dirSign.x < 0.0 ? 1 : 2; } else\n" +
            "   if (lastNormal == 1) { normalIdx = dirSign.y < 0.0 ? 3 : 4; }\n" +
            "   else {                 normalIdx = dirSign.z < 0.0 ? 5 : 6; }\n" +
            "}\n" +
// todo fetch block, and set its color into rgb
// todo texture lookup based on block ID, UV and dUV
            "   float r = float((hitSomething >> 16) & 255)/255.0;\n" +
            "   float g = float((hitSomething >> 8) & 255)/255.0;\n" +
            "   float b = float((hitSomething) & 255)/255.0;\n" +
            "   result = vec4(r, g, b, float(normalIdx)/255.0);\n" +
            "}\n"
) {
    init {
        glslVersion = max(glslVersion, 430)
    }
}