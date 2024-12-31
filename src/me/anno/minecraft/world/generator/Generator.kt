package me.anno.minecraft.world.generator

import me.anno.minecraft.rendering.v2.saveSystem
import me.anno.minecraft.world.Chunk
import org.joml.Vector3i

abstract class Generator {

    abstract fun generate(chunk: Chunk)

    fun loadSaveData(chunk: Chunk) {
        val chunkId = Vector3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ)
        saveSystem.get(chunkId, true) { changesInChunk ->
            for ((blockPos, type) in changesInChunk) {
                chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, type)
            }
        }
    }

}