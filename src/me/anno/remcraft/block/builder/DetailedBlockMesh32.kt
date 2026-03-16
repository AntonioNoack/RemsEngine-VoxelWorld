package me.anno.remcraft.block.builder

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.StaticBuffer
import me.anno.remcraft.rendering.v2.VertexFormat.blockAttributes32Bit
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.AABBf

class DetailedBlockMesh32 : DetailedBlockMesh<IntArray>() {

    override fun ensureBuffer() {
        if (buffer != null) return
        val data = data ?: return
        val buffer = StaticBuffer("detailedBlockMesh32", blockAttributes32Bit, data.size shr 2)
        val nio = buffer.getOrCreateNioBuffer()
        for (i in data.indices) {
            nio.putInt(data[i])
        }
        this.buffer = buffer
    }

    override fun toMesh(): Mesh {
        ensureBuffer()
        val mesh = Mesh()
        val data = data!!
        val positions = FloatArray(data.size.shr(2) * 3)
        forLoopSafely(data.size, 4) { i ->
            val j = i.shr(2) * 3
            positions[j] = data[i] * SCALE
            positions[j + 1] = data[i + 1] * SCALE
            positions[j + 2] = data[i + 2] * SCALE
        }
        mesh.positions = positions
        mesh.materials = materials
        return mesh
    }

    override fun getDataSize(data: IntArray): Int = data.size
    override fun union(data: IntArray, i: Int, dst: AABBf) {
        val x = data[i] * SCALE
        val y = data[i + 1] * SCALE
        val z = data[i + 2] * SCALE
        dst.union(x, y, z)
    }
}