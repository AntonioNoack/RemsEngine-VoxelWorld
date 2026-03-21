package me.anno.remcraft.rendering.globalillumination

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage

object GITextureShader : ECSMeshShader("gi-textured") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "gi-material",
                createFragmentVariables(key),
                concatDefines(key).toString() +
                        // discardByCullingPlane +
                        "vec4 color = texture(diffuseMap,uv);\n" +
                        "finalColor = 0.01 * color.rgb;\n" +
                        "finalAlpha = color.a;\n" +
                        normalTanBitanCalculation +
                        // normalMapCalculation +
                        "finalEmissive = color.rgb * vertexColor0.rgb * emissiveBase;\n" +
                        occlusionCalculation +
                        // metallicCalculation +
                        // roughnessCalculation +
                        // v0 + sheenCalculation +
                        // clearCoatCalculation +
                        // reflectionCalculation +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot)
                .add(ShaderLib.brightness)
                .add(ShaderLib.parallaxMapping)
                .add(RendererLib.getReflectivity)
        )
    }
}