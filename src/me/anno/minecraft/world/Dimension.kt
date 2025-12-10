package me.anno.minecraft.world

import me.anno.cache.Promise
import me.anno.maths.chunks.cartesian.ChunkSystem
import me.anno.minecraft.block.BlockRegistry.Air
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.world.decorator.Decorator
import me.anno.minecraft.world.generator.Generator
import me.anno.utils.pooling.ObjectPool
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import kotlin.math.min

class Dimension(val generator: Generator, val decorators: List<Decorator>) : ChunkSystem<Chunk, BlockType>(5, 5, 5) {

    val gravity = Vector3f(0f, -9.81f, 0f)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int, result: Promise<Chunk>) {
        val t0 = System.nanoTime()
        val chunk = chunkPool.create()
        chunk.set(chunkX * sizeX, chunkY * sizeY, chunkZ * sizeZ)
        chunk.clear()
        generator.generate(chunk)
        val t1 = System.nanoTime()
        // 27ms -> 7ms by using faster noise on Ryzen 5 2600
        // 3.0-3.5ms on Ryzen 9 7950x3D
        if (printTimes) LOGGER.info("gen ${((t1 - t0) * 1e-6).f3()}ms/c")
        result.value = chunk
    }

    override fun getElement(container: Chunk, localX: Int, localY: Int, localZ: Int, index: Int): BlockType {
        return container.getBlock(localX, localY, localZ)
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int): BlockType {
        return getBlockAt(globalX, globalY, globalZ, Int.MAX_VALUE)
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): BlockType {
        return getChunkAt(globalX, globalY, globalZ, stage)
            ?.getBlock(globalX and maskX, globalY and maskY, globalZ and maskZ) ?: Air
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int, chunk: Chunk): BlockType {
        return getBlockAt(globalX, globalY, globalZ, chunk.decorator)
    }

    fun getMetadataAt(globalX: Int, globalY: Int, globalZ: Int): Metadata? {
        return getMetadataAt(globalX, globalY, globalZ, Int.MAX_VALUE)
    }

    fun getMetadataAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): Metadata? {
        return getChunkAt(globalX, globalY, globalZ, stage)
            ?.getMetadata(globalX and maskX, globalY and maskY, globalZ and maskZ)
    }

    fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int, stage: Int): Chunk? {
        val chunk = getChunk(chunkX, chunkY, chunkZ, true)?.waitFor() ?: return null
        for (stage2 in chunk.decorator until min(stage, decorators.size)) {
            decorators[stage2].decorate(chunk)
            chunk.decorator = stage2 + 1
        }
        return chunk
    }

    fun getChunkAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): Chunk? =
        getChunk(globalX shr bitsX, globalY shr bitsY, globalZ shr bitsZ, stage)

    override fun setElement(
        container: Chunk,
        localX: Int, localY: Int, localZ: Int,
        index: Int, element: BlockType
    ): Boolean {
        return container.setBlock(localX, localY, localZ, element)
    }

    fun unload(chunk: Chunk) {
        removeChunk(chunk.chunkX, chunk.chunkY, chunk.chunkZ)?.waitFor { value ->
            if (value != null) chunkPool.destroy(value)
        }
    }

    private val chunkPool = ObjectPool { Chunk(this, 0, 0, 0) }

    companion object {
        private val LOGGER = LogManager.getLogger(Dimension::class)
        var printTimes = false
    }
}