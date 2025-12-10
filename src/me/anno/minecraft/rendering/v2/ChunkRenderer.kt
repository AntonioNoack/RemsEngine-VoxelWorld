package me.anno.minecraft.rendering.v2

import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRendererImpl
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes16Bit
import me.anno.minecraft.rendering.v2.VertexFormat.blockVertexData
import me.anno.utils.types.Floats.roundToIntOr
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
        val pos = mesh.positions!!
        val col = mesh.color0!!
        val buffer = StaticBuffer("chunk$key", attributes, pos.size / 3)
        val nio = buffer.getOrCreateNioBuffer()
        val dx = key.x * csx
        val dy = key.y * csy
        val dz = key.z * csz
        for (i in 0 until buffer.vertexCount) {
            val texId = col[i] - 1 // 0 is air
            val x = dx + pos[i * 3].roundToIntOr()
            val y = dy + pos[i * 3 + 1].roundToIntOr()
            val z = dz + pos[i * 3 + 2].roundToIntOr()
            nio.putShort(x.toShort())
            nio.putShort(y.toShort())
            nio.putShort(z.toShort())
            nio.putShort(texId.toShort())
        }
        buffer.isUpToDate = false
        return buffer to null
    }
}
