package me.anno.minecraft.world

import me.anno.maths.chunks.cartesian.ChunkSystem
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.world.decorator.Decorator
import me.anno.minecraft.world.generator.Generator
import me.anno.utils.pooling.ObjectPool
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3i

class Dimension(val generator: Generator, val decorators: List<Decorator>) : ChunkSystem<Chunk, BlockType>(5, 5, 5) {

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): Chunk {
        val t0 = System.nanoTime()
        val chunk = chunkPool.create()
        chunk.set(chunkX * sizeX, chunkY * sizeY, chunkZ * sizeZ)
        chunk.clear()
        generator.generate(chunk)
        val t1 = System.nanoTime()
        // 27ms -> 7ms by using faster noise on Ryzen 5 2600
        // 3.0-3.5ms on Ryzen 9 7950x3D
        if (printTimes) LOGGER.info("gen ${((t1 - t0) * 1e-6).f3()}ms/c")
        return chunk
    }

    override fun getElement(container: Chunk, localX: Int, localY: Int, localZ: Int, index: Int): BlockType {
        return container.getBlock(localX, localY, localZ)
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): BlockType {
        return getChunkAt(globalX, globalY, globalZ, stage)
            ?.getBlock(globalX and maskX, globalY and maskY, globalZ and maskZ) ?: Air
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int, chunk: Chunk): BlockType {
        return getChunkAt(globalX, globalY, globalZ, chunk.decorator)
            ?.getBlock(globalX and maskX, globalY and maskY, globalZ and maskZ) ?: Air
    }

    fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int, stage: Int): Chunk? {
        val stage1 = minOf(if (stage < 0) Int.MAX_VALUE else stage, decorators.size)
        val chunk = getChunk(chunkX, chunkY, chunkZ, true) ?: return null
        for (stage2 in chunk.decorator until stage1) {
            decorators[stage2].decorate(chunk)
            chunk.decorator = stage2 + 1
        }
        return chunk
    }

    fun getChunkAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int) =
        getChunk(globalX shr bitsX, globalY shr bitsY, globalZ shr bitsZ, stage)

    override fun setElement(
        container: Chunk,
        localX: Int, localY: Int, localZ: Int,
        index: Int, element: BlockType
    ): Boolean {
        return container.setBlock(localX, localY, localZ, element)
    }

    fun unload(chunk: Chunk) {
        val c2 = remove(Vector3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ))
        if (c2 != null) chunkPool.destroy(c2)
    }

    private val chunkPool = ObjectPool { Chunk(this, 0, 0, 0) }

    companion object {
        private val LOGGER = LogManager.getLogger(Dimension::class)
        var printTimes = false
    }
}