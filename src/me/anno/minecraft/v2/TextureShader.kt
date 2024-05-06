package me.anno.minecraft.v2

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object TextureShader : ECSMeshShader("textured") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key)
                    .filter { it.name != "uv" } + listOf(
                    Variable(GLSLType.V1I, "blockIndex"),
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.V2F, "uv", VariableMode.OUT),
                    Variable(GLSLType.S2DA, "diffuseMapArray")
                ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // calculate uv by normal and local position
                        "vec3 lp = localPosition;\n" +
                        "uv =\n" +
                        "   normal.y > +0.5 ? +lp.xz :\n" +
                        "   normal.y < -0.5 ? vec2(-lp.x,lp.z) :\n" +
                        "   normal.x > +0.5 ? -lp.zy :\n" +
                        "   normal.x < -0.5 ? vec2(lp.z,-lp.y) :\n" +
                        "   normal.z > +0.5 ? vec2(lp.x,-lp.y) :\n" +
                        "                     -lp.xy;\n" +
                        "vec4 color = texture(diffuseMapArray, vec3(uv,float(blockIndex)));\n" +
                        "if(color.a < ${1f / 255f}) discard;\n" +
                        "finalColor = color.rgb;\n" +
                        "finalAlpha = color.a;\n" +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionCalculation +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot)
                .add(ShaderLib.brightness)
                .add(ShaderLib.parallaxMapping)
                .add(RendererLib.getReflectivity)
        )
    }
}