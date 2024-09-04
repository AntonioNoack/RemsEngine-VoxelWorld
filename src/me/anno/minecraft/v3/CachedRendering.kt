package me.anno.minecraft.v3

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.systems.OnUpdate
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.minecraft.block.BlockType.Companion.Dirt
import me.anno.minecraft.block.BlockType.Companion.Grass
import me.anno.minecraft.block.BlockType.Companion.Stone
import me.anno.minecraft.world.Dimension
import me.anno.utils.structures.lists.Lists.createList
import kotlin.math.max

class CachedRendering(dimension: Dimension, entity0: Entity) : Component(), OnUpdate {

    companion object {
        val NUM_CHUNK_BUFFERS = 1 // round-robin
        val CHUNK_SIZE = 32 // 32 ~ 32 kiB, 64 ~ 256 kiB
        val CHUNKS_PER_FRAME = 32
        val MIN_Y = 0
        val MAX_Y = 64
        val DELTA_Y = MAX_Y - MIN_Y

        // index: (x + z * sx) * sy + y
        val getIndexXZ = "uint getXZIndex(ivec2 xz){\n" +
                "   xz &= ${CHUNK_SIZE - 1};\n" +
                "   return uint(xz.x + xz.y * $CHUNK_SIZE);\n" +
                "}\n"
        val getIndex = "uint getIndex(uint xzIndex, uint y){\n" +
                "   y %= ${DELTA_Y - 1};\n" +
                "   return xzIndex * $DELTA_Y + y;\n" +
                "}\n"

        private val attributes = listOf(Attribute("block", AttributeType.UINT32, 1, true))
        val TOTAL_CHUNK_SIZE = CHUNK_SIZE * DELTA_Y * CHUNK_SIZE
        val chunkBuffers = createList(NUM_CHUNK_BUFFERS) {
            ComputeBuffer("buffer", attributes, TOTAL_CHUNK_SIZE)
        }

        val helper = Framebuffer(
            "helper", CHUNK_SIZE, CHUNK_SIZE,
            1, TargetType.UInt8x1, DepthBufferType.NONE
        )

        val generationShader = Shader(
            "chunkGen", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.V2I, "xzOffset"),
                Variable(GLSLType.V1I, "step"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "layout(std430, binding = 0) writeonly buffer blockBuffer { uint blocks[]; };\n" +
                    getIndexXZ + getIndex +
                    "int dotI(ivec3 a){\n" +
                    "   return a.x*a.x + a.y*a.y + a.z*a.z;\n" +
                    "}\n" +
                    // todo implement proper world-gen or Kotlin-to-GLSL translator for this
                    "void main() {\n" +
                    "   ivec2 relBlockPos = ivec2(gl_FragCoord.xy);\n" +
                    "   if (relBlockPos.x < 0 || relBlockPos.x >= $CHUNK_SIZE || " +
                    "       relBlockPos.y < 0 || relBlockPos.y >= $CHUNK_SIZE) discard;\n" +
                    "   uint xzIndex = getXZIndex(relBlockPos);\n" +
                    "   ivec2 blockPos = xzOffset + relBlockPos * step;\n" +
                    "   int height = 30 + int(20.0 * sin(dot(blockPos,blockPos) * 0.0003));\n" +
                    "   uint maxHeight = 0u;\n" +
                    "   for(uint yi=0u;yi<$MAX_Y;yi++){\n" +
                    "       int y = int(yi) + $MIN_Y;\n" +
                    "       uint block = y < height ? y < height-4 ? ${Stone.color}u : y < height-1 ? ${Dirt.color}u : ${Grass.color}u : 0u;\n" +
                    "       blocks[getIndex(xzIndex,yi)] = block;\n" +
                    "       if(block != 0u) maxHeight = yi;\n" +
                    "   }\n" +
                    "" +
                    // we can draw the max height here
                    "   result = vec4(vec3(float(maxHeight)/float(${DELTA_Y - 1})),1.0);\n" +
                    "}"
        ).apply { glslVersion = max(glslVersion, 430) }

        val isOutOfBounds = "any(lessThan(blockPosition, vec3(0.0))) || any(greaterThan(blockPosition, bounds1))"
    }


    // todo
    //   - distant chunks only need to be rendered every once in a while, cache them on CubeMaps, maybe 2048x6, or multi-sampled 1024x6, or generally blurry 512
    //   - very distant chunks don't need logic, so generate them on the GPU before rendering
    //   - only render them, if the raw-movement error is 10px or so,
    //     - and just reproject them before that (move the big box slightly)

    // todo Raster-Pipeline for close chunks:
    //  - generate chunk with full resolution into a buffer
    //  - render each visible block (could be many!!) onto the texture

    // todo Compute-Pipeline for distant chunks:
    //  - generate chunk, maybe with lower grid resolution, in shared memory in a 64x64 work group [could also be 2+ passes]
    //  - synchronize
    //  - rasterize chunk:
    //    - if blocks are far enough, just project their positions and their colors onto a pre-buffer, and then onto the screen
    //  - block-trace:
    //   -  for the target image on each relevant pixel, block-trace

    // todo to get started, generate the chunks on CPU, and then upload them as raw data instead of triangulized

    // do we need two framebuffers per LOD?? yes
    //  - one for current rendering, one for previous???

    // todo render LOD 0 with proper triangle mesh

    val NUM_LODS = 3
    val lods = createList(NUM_LODS) {
        // todo LODs with step are not correctly positioned or generated :/
        val step = 1 shl max(0, it - 2)
        val min = 2 * (1 shl it) / step // @ chunkSize = 32
        CachedLOD(
            dimension, step,
            if (it == 0) 0 else min,
            min * 2
        )
    }

    init {
        for (i in 0 until NUM_LODS) {
            val lod = lods[i]
            entity0.add(
                Entity("LOD[$i]")
                    .setScale(lod.meshScale)
                    .add(lod).add(lod.mesh)
            )
        }
    }

    private val timer = GPUClockNanos()
    override fun onUpdate() {
        timeRendering("CachedRendering", timer) {
            for (lod in lods) {
                lod.update()
            }
        }
    }

}