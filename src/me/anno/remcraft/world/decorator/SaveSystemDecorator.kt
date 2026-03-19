package me.anno.remcraft.world.decorator

import me.anno.remcraft.rendering.v2.saveSystem
import me.anno.remcraft.world.Chunk
import org.joml.Vector3i
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class SaveSystemDecorator: Decorator() {
    override fun decorate(chunk: Chunk) {
        val chunkId = Vector3i(chunk.xi, chunk.yi, chunk.zi)
        saveSystem.get(chunkId) { changesInChunk ->
            for ((index, type) in changesInChunk) {
                chunk.setBlock(index, type)
            }
        }
    }
}