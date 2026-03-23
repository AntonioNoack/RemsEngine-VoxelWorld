package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.buffer.GPUBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import org.joml.Vector3f

object GIDirectSunTextureShader : ECSMeshShader("gi-textured") {

    var chunkMap = 0
    lateinit var chunkBuffer: GPUBuffer

    val sunDir = Vector3f()
    val sunColor = Vector3f()

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)

        shader.v3f("sunDir", sunDir)
        shader.v3f("sunColor", sunColor)
        shader.v3f("cameraPosition", RenderState.cameraPosition)

        shader.v1i("maxSteps", 100)
        shader.v1i("chunkMap", chunkMap)
        shader.bindBuffer(0, chunkBuffer)
    }

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
                        listOf(
                            Variable(GLSLType.V3F, "lightLevel"),
                            Variable(GLSLType.V3F, "sunDir"),
                            Variable(GLSLType.V3F, "sunColor"),
                            Variable(GLSLType.V3F, "cameraPosition"),
                            Variable(GLSLType.V3F, "finalPosition"),
                            Variable(GLSLType.V1I, "chunkMap"),
                            Variable(GLSLType.V1I, "maxSteps"),
                            Variable(GLSLType.BUFFER, "chunkData")
                                .defineBufferFormat("int[] ChunkData;")
                                .binding(0),
                        ),
                concatDefines(key).toString() +
                        // todo make leaves properly transparent...
                        "vec4 color = texture(diffuseMap,uv);\n" +
                        "finalColor = vec3(0.0);\n" +
                        "finalAlpha = color.a;\n" +
                        normalTanBitanCalculation +
                        // run sun-tracing for primary ray for 100% sharp shadows
                        "float hitDistance = 0.0; ivec4 hitFace = ivec4(0); int hitChunkData = 0;\n" +
                        "vec3 lightLevelI = lightLevel;\n" +
                        "vec3 pos = finalPosition + cameraPosition + normal * 0.01;\n" +
                        "if (dot(sunDir, normal) > 0.0) {\n" +
                        "   float opacity = blockTracing(pos,sunDir,hitDistance,hitFace,hitChunkData);\n" +
                        "   lightLevelI += sunColor * dot(sunDir, normal) * (1.0 - opacity);\n" +
                        "}\n" +
                        "finalEmissive = color.rgb * (lightLevelI + 0.001);\n" +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot)
                .add(ShaderLib.brightness)
                .add(ShaderLib.parallaxMapping)
                .add(RendererLib.getReflectivity)
                .add(hashMap)
                .add(randomness)
                .add(blockTracing)
        )
    }
}