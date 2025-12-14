package me.anno.minecraft.rendering.v2

import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRendererImpl
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.minecraft.block.builder.DetailedBlockMesh32
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes32Bit
import me.anno.minecraft.rendering.v2.VertexFormat.detailsBlockVertexData
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3i

class DetailChunkRenderer(val material: Material) :
    UniqueMeshRendererImpl<Vector3i, DetailedBlockMesh32>(
        blockAttributes32Bit,
        detailsBlockVertexData,
        false,
        DrawMode.TRIANGLES
    ) {

    override val hasVertexColors: Int get() = 1
    override val materials: List<FileReference> = FileCacheList.of(material)

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        dstUnion.all()
        localAABB.all()
        globalAABB.all()
    }

    /**
     * defines what the world looks like for Raycasting, and for AABBs
     * */
    override fun getTransformAndMaterial(key: Vector3i, transform: Transform): Material = material

    override fun createBuffer(key: Vector3i, mesh: DetailedBlockMesh32): Pair<StaticBuffer, IntArray?>? {
        if (mesh.numPrimitives == 0L) return null
        mesh.ensureBuffer()
        val buffer = mesh.buffer ?: return null
        return buffer to null
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val component = DetailChunkRenderer(TextureMaterial.solid)
            testSceneWithUI("DetailChunkRenderer", component)
        }
    }
}
