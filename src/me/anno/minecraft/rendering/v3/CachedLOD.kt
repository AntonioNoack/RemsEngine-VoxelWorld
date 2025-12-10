package me.anno.minecraft.rendering.v3

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.CubemapTexture
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.sq
import me.anno.maths.patterns.SpiralPattern
import me.anno.mesh.Shapes.flatCube
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.CHUNKS_PER_FRAME
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.CHUNK_SIZE
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.DELTA_Y
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.MAX_Y
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.MIN_Y
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.chunkBuffers
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.generationShader
import me.anno.minecraft.rendering.v3.CachedRendering.Companion.helper
import me.anno.minecraft.world.Dimension
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3i
import kotlin.math.max

// todo we could skip chunks, where we don't look... is complicated with being finished or not though
class CachedLOD(
    val dimension: Dimension, val step: Int,
    val minChunkDistance: Int, val maxChunkDistance: Int // e.g. 3-6, so draw -5,-4,-3,[-2,-1,0,1,2],3,4,5
) : Component() {

    val material = Material()
    val mesh = MeshComponent(flatCube.back, material)
    val unitSize = CHUNK_SIZE * step
    val meshScale = maxChunkDistance * unitSize * 2f

    // val edgeSize = maxChunkDistance * 2 - 1
    // val chunks = BooleanArrayList(edgeSize * edgeSize)

    // generate loading pattern
    // todo could be produced dynamically...
    val loadingPattern = (minChunkDistance until maxChunkDistance)
        .flatMap { SpiralPattern.spiral2d(it, 0, false) }
        .sortedBy { it.lengthSquared() }
        .map { it.mul(unitSize).sub(unitSize.shr(1), unitSize.shr(1), unitSize.shr(1)) }

    @DebugProperty
    var patternIndex = 0

    // "depth" could use max-operation on color,
    //  DRGB, using 32 bits/pixel, and RGB being 8x3 or 565
    // -> we only need depth, when we don't know the ordering,
    // we know the ordering, so just draw front to back, and we won't need any depth-testing
    // todo -> and for improved performance, we could skip lanes/chunks completely by a quick all-depth-test in the future

    val renderPosition = Vector3i()
    var renderingFB = CubemapFramebuffer("chunk", 1024, 1, TargetType.UInt8x4.wrap(), DepthBufferType.NONE)
    var presentedFB = CubemapFramebuffer("chunk", 1024, 1, TargetType.UInt8x4.wrap(), DepthBufferType.NONE)

    // save, which blocks need to be updated
    //  -> always is the same pattern??? no, because of 1:1 chunks (false), and down-scaled chunks (true)
    @DebugProperty
    val invalidationDistanceSq = sq(max((step * minChunkDistance * CHUNK_SIZE) * 0.02, 1.5))

    init {
        renderPosition.set(0, 0, 0)
    }

    fun swap() {
        val tmp = renderingFB
        renderingFB = presentedFB
        presentedFB = tmp

        material.shader = CubemapModelShader
        material.shaderOverrides["colorTex"] = TypeValue(GLSLType.SCube, presentedFB.getTexture0())

        val transform = transform!!
        transform.localPosition = transform.localPosition
            .set(renderPosition)
        transform.teleportUpdate()
        invalidateBounds()
    }

    fun update() {

        // if |renderPosition - playerPosition| > 10px threshold @1080p
        //  then clear cubemap, and restart
        val cp = RenderView.currentInstance?.cameraPosition
        val rp = renderPosition
        if (cp != null &&
            ((patternIndex == loadingPattern.size &&
                    cp.distanceSquared(rp.x.toDouble(), rp.y.toDouble(), rp.z.toDouble()) > invalidationDistanceSq) ||
                    Input.wasKeyPressed(Key.KEY_O))
        ) {
            patternIndex = 0
            renderPosition.set(cp.x.roundToIntOr(), cp.y.roundToIntOr(), cp.z.roundToIntOr())
        }

        //  render N chunks onto the screen, close first
        for (i in 0 until CHUNKS_PER_FRAME) {
            if (generateChunk()) break
        }
    }

    fun generateChunk(): Boolean {
        if (patternIndex < loadingPattern.size) {
            val isFirst = patternIndex == 0
            val (cx, _, cz) = loadingPattern[patternIndex++]
            val buffer = chunkBuffers[0]
            buffer.ensureBufferWithoutResize()
            // blocks only need to be generated once, so reuse them
            useFrame(helper, Renderer.copyRenderer) {
                // bind generation shader
                val shader = generationShader
                shader.use()
                shader.v2i("xzOffset", renderPosition.x + cx, renderPosition.z + cz)
                shader.v1i("step", step)
                shader.bindBuffer(0, buffer)
                // draw generation shader
                flat01.draw(shader)
            }
            val skyRot = Quaternionf()
            val transform = Matrix4f()
            val model = Matrix4x3f()
            GFXState.blendMode.use(null) {
                GFXState.cullMode.use(CullMode.FRONT) {
                    renderingFB.draw(Renderer.copyRenderer) { side: Int ->
                        if (isFirst) renderingFB.clearColor(0)
                        // todo calculate whether the chunk appears on this side
                        // if so, render these bounds using block-tracing
                        val shader = DrawingShader
                        shader.use()
                        renderingFB.getTexture0().bindTrulyNearest(shader, "resultTex")
                        CubemapTexture.rotateForCubemap(skyRot.identity(), side)
                        val sx = CHUNK_SIZE// * step
                        val scale = minChunkDistance.toFloat() * sx
                        Perspective.setPerspective(
                            transform, Maths.PIf * 0.5f, 1f,
                            scale * 0.5f, scale * 3f, 0f, 0f
                        )
                        transform.rotate(skyRot)
                        val y0 = (MAX_Y + MIN_Y) * 0.5f - renderPosition.y
                        val st = 1f / step
                        val ts = 0.5f * step
                        model.identity()
                            .setTranslation(cx.toFloat(), y0, cz.toFloat())
                            .scale(sx * ts, DELTA_Y * ts, sx * ts)
                        shader.m4x4("transform", transform)
                        shader.m4x3("localTransform", model)
                        shader.v3f("cameraPosition", -cx.toFloat() * st, -y0 * st, -cz.toFloat() * st)
                        flatCube.back.draw(null, shader, 0)
                    }
                }
            }
            if (patternIndex == loadingPattern.size) {
                swap()
            }
            return false
        } else return true
    }

}