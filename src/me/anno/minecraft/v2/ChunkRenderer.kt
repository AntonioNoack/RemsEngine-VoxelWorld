package me.anno.minecraft.v2

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.maths.Maths
import org.joml.Vector3i

class ChunkRenderer(val material: Material) : UniqueMeshRenderer<Vector3i>(
    if (material is TextureMaterial) blockAttributes2
    else blockAttributes, blockVertexData,
    material, DrawMode.TRIANGLES
) {

    override val hasVertexColors: Int get() = 1

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
     * */
    override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
        var i = 0
        for ((key, entry) in entryLookup) {
            val transform = getTransform(i++)
            transform.setLocalPosition(
                (key.x * csx).toDouble(),
                (key.y * csy).toDouble(),
                (key.z * csz).toDouble(),
            )
            run(entry.mesh!!, material, transform)
        }
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
        val conversion = material !is TextureMaterial
        for (i in 0 until buffer.vertexCount) {
            data.putFloat(dx + pos[i * 3])
            data.putFloat(dy + pos[i * 3 + 1])
            data.putFloat(dz + pos[i * 3 + 2])
            data.putInt(
                if (conversion) Maths.convertABGR2ARGB(col[i])
                else col[i] - 1 // 0 is reserved to be air
            )
        }
        buffer.isUpToDate = false
        return buffer
    }
}
