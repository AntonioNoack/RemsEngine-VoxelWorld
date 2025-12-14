package me.anno.minecraft.rendering.v3

import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.engine.ui.render.Frustum
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.files.FileReference
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes16Bit
import me.anno.minecraft.rendering.v2.VertexFormat.blockVertexData
import me.anno.minecraft.rendering.v3.ChunkMesh.Companion.emptyRange
import me.anno.utils.types.Ranges.size
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3
import org.joml.Vector3i

class ChunkRenderer2(val material: Material) :
    UniqueMeshRenderer<Vector3i, ChunkMesh>(blockAttributes16Bit, blockVertexData, false, DrawMode.TRIANGLES) {

    companion object {
        val infiniteBounds = AABBf().all()
    }

    override val hasVertexColors: Int get() = 1
    override fun getBounds(): AABBf = infiniteBounds

    override val materials: List<FileReference> = FileCacheList.of(material)

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        dstUnion.all()
        localAABB.all()
        globalAABB.all()
    }

    override fun forEachMesh(
        pipeline: Pipeline?,
        callback: (IMesh, Material?, Transform) -> Boolean
    ) {
        val transform = transform ?: Transform()
        for (entry in values) {
            callback(entry, material, transform)
        }
    }

    override fun getVertexRange(mesh: ChunkMesh): IntRange = mesh.vertexRange
    override fun setVertexRange(mesh: ChunkMesh, value: IntRange) {
        mesh.vertexRange = value
    }

    override fun getIndexRange(mesh: ChunkMesh): IntRange = emptyRange
    override fun setIndexRange(mesh: ChunkMesh, value: IntRange) {}

    override fun insertVertexData(from: Int, fromData: ChunkMesh, to: IntRange, toData: StaticBuffer) {
        check(from == 0)
        toData.uploadElementsPartially(
            from, fromData.buffer,
            to.size, to.first
        )
    }

    override fun insertIndexData(from: Int, fromData: ChunkMesh, to: IntRange, toData: StaticBuffer) = Unit
    override fun shallRenderEntry(frustum: Frustum?, transform: Matrix4x3?, entry: ChunkMesh): Boolean {
        if (frustum == null || transform == null) return true
        val bounds = AABBd(entry.getBounds())
        bounds.transform(transform)
        return frustum.contains(bounds)
    }
}
