package me.anno.minecraft.rendering.v3

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.minecraft.rendering.v2.*
import me.anno.minecraft.rendering.v2.ChunkLoaderBase.Companion.mapPalette
import me.anno.minecraft.world.Chunk
import me.anno.utils.types.Ranges.size
import org.joml.AABBf

class ChunkMesh(
    val chunk: Chunk,
    solidRenderer: ChunkRenderer2,
    blockFilter: BlockFilter
) : IMesh {

    companion object {
        @Suppress("EmptyRange")
        val emptyRange = 0 until 0
        val chunkBounds = AABBf(
            0f, 0f, 0f,
            dimension.sizeX.toFloat(),
            dimension.sizeY.toFloat(),
            dimension.sizeZ.toFloat()
        )
    }

    val buffer = run {
        val palette = if (solidRenderer.material is TextureMaterial) {
            mapPalette { it.texId + 1 }
        } else {
            mapPalette { it.color }
        }
        // todo use faster, without-intermediate-mesh method
        ChunkLoaderModel(chunk)
            .createBuffer(palette, blockFilter)
    }

    var vertexRange = emptyRange

    override val numPrimitives: Long
        get() = vertexRange.size.toLong() / 3

    override fun ensureBuffer() {}
    override fun getBounds(): AABBf = chunkBounds

    override fun draw(
        pipeline: Pipeline?,
        shader: Shader,
        materialIndex: Int,
        drawLines: Boolean
    ) {
        throw NotImplementedError()
    }

    override fun drawInstanced(
        pipeline: Pipeline,
        shader: Shader,
        materialIndex: Int,
        instanceData: Buffer,
        drawLines: Boolean
    ) {
        throw NotImplementedError()
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        throw NotImplementedError()
    }
}