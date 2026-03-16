package me.anno.remcraft.world.generator

import me.anno.remcraft.rendering.v2.saveSystem
import me.anno.remcraft.world.Chunk
import org.joml.Vector3i

abstract class Generator {

    abstract fun generate(chunk: Chunk)

    fun loadSaveData(chunk: Chunk) {
        val chunkId = Vector3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ)
        saveSystem.get(chunkId) { changesInChunk ->
            for ((index, type) in changesInChunk) {
                chunk.setBlock(index, type)
            }
        }
    }

}