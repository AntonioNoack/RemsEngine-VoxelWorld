package me.anno.minecraft.block.builder

import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.minecraft.rendering.v2.TextureMaterial
import me.anno.minecraft.rendering.v2.VertexFormat.detailsBlockVertexData
import org.joml.AABBf

abstract class DetailedBlockMesh<DataArray> : PrefabSaveable(), IMesh {

    companion object {
        const val SCALE = 1f / 16
        val pseudoComponent = MeshComponent()
        val materialsI = FileCacheList.of(TextureMaterial.solid)
    }

    // x,y,z,texId
    var data: DataArray? = null

    var validBounds = false
    val calcBounds = AABBf()

    var buffer: StaticBuffer? = null

    override var materials: List<FileReference> = materialsI

    abstract fun calculateBounds()

    override fun getBounds(): AABBf {
        if (!validBounds) calculateBounds()
        return calcBounds
    }

    override fun draw(
        pipeline: Pipeline?,
        shader: Shader,
        materialIndex: Int,
        drawLines: Boolean
    ) {
        ensureBuffer()
        val buffer = buffer ?: return
        buffer.draw(shader, DrawMode.TRIANGLES)
    }

    override fun drawInstanced(
        pipeline: Pipeline,
        shader: Shader,
        materialIndex: Int,
        instanceData: Buffer,
        drawLines: Boolean
    ) {
        ensureBuffer()
        val buffer = buffer ?: return
        buffer.drawInstanced(shader, instanceData, DrawMode.TRIANGLES)
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        pipeline.addMesh(this, pseudoComponent, transform)
    }

    override val vertexData: MeshVertexData
        get() = detailsBlockVertexData

}