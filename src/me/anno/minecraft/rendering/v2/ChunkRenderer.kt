package me.anno.minecraft.rendering.v2

import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRendererImpl
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes16Bit
import me.anno.minecraft.rendering.v2.VertexFormat.blockVertexData
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3i

class ChunkRenderer(val material: Material) :
    UniqueMeshRendererImpl<Vector3i, Mesh>(blockAttributes16Bit, blockVertexData, false, DrawMode.TRIANGLES) {

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
    override fun getTransformAndMaterial(key: Vector3i, transform: Transform): Material {
        transform.setLocalPosition(
            (key.x * csx).toDouble(),
            (key.y * csy).toDouble(),
            (key.z * csz).toDouble(),
        )
        transform.teleportUpdate()
        return material
    }

    override fun createBuffer(key: Vector3i, mesh: Mesh): Pair<StaticBuffer, IntArray?>? {
        if (mesh.numPrimitives == 0L) return null
        val pos = mesh.positions ?: return null
        val buffer = StaticBuffer("chunk$key", attributes, pos.size / 3)
        val nio = ChunkLoaderModel.createBuffer(key.x * csx, key.y * csy, key.z * csz, mesh)
        buffer.nioBuffer = nio
        buffer.cpuSideChanged()
        return buffer to null
    }
}
