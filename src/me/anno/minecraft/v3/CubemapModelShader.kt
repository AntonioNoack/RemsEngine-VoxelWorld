package me.anno.minecraft.v3

import me.anno.engine.ui.render.ECSMeshShader.Companion.discardByCullingPlane
import me.anno.engine.ui.render.ECSMeshShader.Companion.finalMotionCalculation
import me.anno.engine.ui.render.ECSMeshShaderLight
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.utils.types.Booleans.hasFlag

object CubemapModelShader : ECSMeshShaderLight("CubemapModelShader") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) + listOf(
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.SCube, "colorTex")
                ), concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        "vec4 texDiffuseMap = texture(colorTex, -localPosition*$cubemapsAreLeftHanded);\n" +
                        "vec4 color = texDiffuseMap;\n" +
                        "if(color.a == 0.0) discard;\n" +
                        "finalColor = color.rgb;\n" +
                        "finalAlpha = 1.0;\n" +
                        "gl_FragDepth = 0.000001;\n" + // todo what should we choose here?
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    "int normalIdx = int(color.a*255.0);\n" +
                                    "finalNormal = " +
                                    "   normalIdx == 1 ? vec3(-1.0,0.0,0.0) :" +
                                    "   normalIdx == 2 ? vec3(+1.0,0.0,0.0) :" +
                                    "   normalIdx == 3 ? vec3(0.0,-1.0,0.0) :" +
                                    "   normalIdx == 4 ? vec3(0.0,+1.0,0.0) :" +
                                    "   normalIdx == 5 ? vec3(0.0,0.0,-1.0) :" +
                                    "                    vec3(0.0,0.0,+1.0);\n" +
                                    "finalNormal = -finalNormal;\n"
                        } else "") +
                        finalMotionCalculation
            )
        )
    }
}