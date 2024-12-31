package me.anno.minecraft.rendering.v2

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes
import me.anno.minecraft.rendering.v2.VertexFormat.blockVertexData
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3i
import java.nio.ByteBuffer

class ChunkRenderer(val material: Material) :
    UniqueMeshRenderer<Mesh, Vector3i>(blockAttributes, blockVertexData, DrawMode.TRIANGLES) {

    override val hasVertexColors: Int get() = 1
    override val materials: List<FileReference> = listOf(material.ref)

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        localAABB.all()
        globalAABB.all()
        return true
    }

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
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

    override fun getData(key: Vector3i, mesh: Mesh): StaticBuffer? {
        if (mesh.numPrimitives == 0L) return null
        val pos = mesh.positions!!
        val col = mesh.color0!!
        val buffer = StaticBuffer("chunk$key", attributes, pos.size / 3)
        val data = buffer.nioBuffer!!
        val dx = key.x * csx
        val dy = key.y * csy
        val dz = key.z * csz
        for (i in 0 until buffer.vertexCount) {
            val blockIndex = col[i] - 1 // 0 is air
            putVertex(data, dx + pos[i * 3], dy + pos[i * 3 + 1], dz + pos[i * 3 + 2], blockIndex)
        }
        buffer.isUpToDate = false
        return buffer
    }

    companion object {
        fun putVertex(data: ByteBuffer, x: Float, y: Float, z: Float, blockIndex: Int) {
            data.putShort(x.roundToIntOr().toShort())
            data.putShort(y.roundToIntOr().toShort())
            data.putShort(z.roundToIntOr().toShort())
            data.putShort(blockIndex.toShort())
        }
    }
}
