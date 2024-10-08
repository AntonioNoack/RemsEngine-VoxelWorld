package me.anno.minecraft.v2

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import org.joml.Vector3i
import kotlin.math.round

class ChunkRenderer(val material: Material) :
    UniqueMeshRenderer<Mesh, Vector3i>(blockAttributes, blockVertexData, DrawMode.TRIANGLES) {

    override val hasVertexColors: Int get() = 1
    override val materials: List<FileReference> = listOf(material.ref)

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
            data.putShort(round(dx + pos[i * 3]).toInt().toShort())
            data.putShort(round(dy + pos[i * 3 + 1]).toInt().toShort())
            data.putShort(round(dz + pos[i * 3 + 2]).toInt().toShort())
            data.putShort((col[i] - 1).toShort()) // 0 is air
        }
        buffer.isUpToDate = false
        return buffer
    }
}
