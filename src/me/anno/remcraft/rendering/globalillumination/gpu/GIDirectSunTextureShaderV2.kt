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
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsY
import me.anno.remcraft.world.Index.bitsZ
import org.joml.Vector3f

object GIDirectSunTextureShaderV2 : ECSMeshShader("gi-textured") {

    var chunkMap = 0
    lateinit var chunkBuffer: GPUBuffer
    lateinit var faceBuffer: GPUBuffer

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
        shader.bindBuffer(1, faceBuffer)
    }

    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        return super.createVertexStages(key) + ShaderStage(
            "faceIds", listOf(
                Variable(GLSLType.V4I, "faceIds", VariableMode.ATTR),
                Variable(GLSLType.V4I, "faceId", VariableMode.OUT)
            ), "faceId = faceIds;\n"
        )
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "gi-material",
                createFragmentVariables(key) +
                        listOf(
                            Variable(GLSLType.V4I, "faceId"),
                            Variable(GLSLType.V3F, "sunDir"),
                            Variable(GLSLType.V3F, "sunColor"),
                            Variable(GLSLType.V3F, "cameraPosition"),
                            Variable(GLSLType.V3F, "finalPosition"),
                            Variable(GLSLType.V3F, "localPosition"),
                            Variable(GLSLType.V1I, "chunkMap"),
                            Variable(GLSLType.V1I, "maxSteps"),
                            Variable(GLSLType.BUFFER, "chunkData")
                                .defineBufferFormat("int[] ChunkData;")
                                .binding(0),
                            Variable(GLSLType.BUFFER, "faceData")
                                .defineBufferFormat("uvec4[] FaceData;")
                                .binding(1),
                        ),
                concatDefines(key).toString() +
                        // todo make leaves properly transparent...
                        "vec4 color = texture(diffuseMap,uv);\n" +
                        "finalColor = vec3(0.0);\n" +
                        "finalAlpha = color.a;\n" +
                        normalTanBitanCalculation +
                        // query lighting from face-data
                        // todo interpolation...
                        "HashMap chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);\n" +
                        "ivec3 blockPos = faceId.xyz;\n" +
                        "ivec3 chunkId = blockPos >> ivec3($bitsX,$bitsY,$bitsZ);\n" +
                        "int chunkData0 = HashMapGet(chunkHashMap, HashChunkId(chunkId));\n" +
                        "vec3 lightLevel = vec3(0.0, 0.0, 1.0);\n" +
                        "if (chunkData0 != -1) {\n" +
                        "   HashMap faceMap = HashMap(ChunkData[chunkData0], chunkData0+1);\n" +
                        "   int sideId = faceId.w;\n" +
                        "   int faceId1 = HashMapGet(faceMap, encodeSideLocal(faceId));\n" +
                        "   if (faceId1 != -1) {\n" +
                        "       vec4 rawColor = vec4(FaceData[faceId1]);\n" +
                        // remove direct sunlight
                        "       float sunFactor = rawColor.a / max(sunColor.r,max(sunColor.g,sunColor.b));\n" +
                        "       lightLevel = rawColor.rgb - sunColor * sunFactor;\n" +
                        "       lightLevel = max(lightLevel, vec3(0.0));\n" +
                        "       lightLevel *= 0.0001;\n" +
                        "   }\n" +
                        "}\n" +

                        // run sun-tracing for primary ray for 100% sharp shadows
                        "float hitDistance = 0.0; ivec4 hitFace = ivec4(0); int hitChunkData = 0;\n" +
                        "vec3 lightLevelI = lightLevel;\n" +
                        "vec3 pos = localPosition + normal * 0.01;\n" +
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
                .add(getSide)
        )
    }
}