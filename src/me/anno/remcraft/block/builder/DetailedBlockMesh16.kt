package me.anno.remcraft.block.builder

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticBuffer
import me.anno.remcraft.rendering.v2.VertexFormat.blockAttributes16Bit
import org.joml.AABBf

class DetailedBlockMesh16 : DetailedBlockMesh<ShortArray>() {

    override fun ensureBuffer() {
        if (buffer != null) return
        val data = data ?: return
        val buffer = StaticBuffer("detailedBlockMesh16", blockAttributes16Bit, data.size shr 2)
        val nio = buffer.getOrCreateNioBuffer()
        for (i in data.indices) {
            nio.putShort(data[i])
        }
        this.buffer = buffer
    }

    override fun toMesh(): Mesh {
        ensureBuffer()
        val mesh = Mesh()
        val data = data!!
        val vertexCount = data.size.shr(2)
        val positions = FloatArray(vertexCount * 3)
        val texIds = ShortArray(vertexCount)
        repeat(vertexCount) { k ->
            val srcI = k * 4
            val dstI = k * 3
            positions[dstI] = data[srcI] * SCALE
            positions[dstI + 1] = data[srcI + 1] * SCALE
            positions[dstI + 2] = data[srcI + 2] * SCALE
            texIds[k] = data[srcI + 3]
        }
        mesh.positions = positions
        mesh.materials = materials
        val texIdsAttr = Attribute("texIds", AttributeType.SINT16, 1)
        mesh.setAttr("texIds", texIds, texIdsAttr)
        return mesh
    }

    override fun getDataSize(data: ShortArray): Int = data.size
    override fun union(data: ShortArray, i: Int, dst: AABBf) {
        val x = data[i] * SCALE
        val y = data[i + 1] * SCALE
        val z = data[i + 2] * SCALE
        dst.union(x, y, z)
    }
}