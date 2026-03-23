package me.anno.remcraft.rendering.globalillumination

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object GITextureShader : ECSMeshShader("gi-textured") {
    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        return super.createVertexStages(key) + ShaderStage(
            "lightLevel", listOf(
                Variable(GLSLType.V3F, "lightLevels", VariableMode.ATTR),
                Variable(GLSLType.V3F, "lightLevel", VariableMode.OUT)
            ), "lightLevel = lightLevels;\n"
        )
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "gi-material",
                createFragmentVariables(key) +
                        Variable(GLSLType.V3F, "lightLevel"),
                concatDefines(key).toString() +
                        "vec4 color = texture(diffuseMap,uv);\n" +
                        "finalColor = vec3(0.0);\n" +
                        "finalAlpha = color.a;\n" +
                        normalTanBitanCalculation +
                        "finalEmissive = color.rgb * (lightLevel + 0.001);\n" +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot)
                .add(ShaderLib.brightness)
                .add(ShaderLib.parallaxMapping)
                .add(RendererLib.getReflectivity)
        )
    }
}