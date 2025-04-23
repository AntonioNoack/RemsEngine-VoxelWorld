package me.anno.minecraft.block.builder

import me.anno.gpu.buffer.StaticBuffer
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes
import me.anno.utils.algorithms.ForLoop.forLoopSafely

class DetailedBlockMesh16 : DetailedBlockMesh<ShortArray>() {

    override val numPrimitives: Long
        get() {
            val data = data ?: return 0
            return (data.size shr 2).toLong()
        }

    override fun ensureBuffer() {
        if (buffer != null) return
        val data = data ?: return
        val buffer = StaticBuffer("detailedBlockMesh16", blockAttributes, data.size shr 2)
        for (i in data.indices) {
            buffer.putShort(data[i])
        }
        this.buffer = buffer
    }

    override fun calculateBounds() {
        val data = data ?: return
        calcBounds.clear()
        forLoopSafely(data.size, 4) { i ->
            val x = data[i] * SCALE
            val y = data[i + 1] * SCALE
            val z = data[i + 2] * SCALE
            calcBounds.union(x, y, z)
        }
        validBounds = true
    }

}