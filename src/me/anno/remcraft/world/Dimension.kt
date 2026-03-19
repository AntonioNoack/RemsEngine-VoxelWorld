package me.anno.remcraft.world

import me.anno.cache.CacheSection
import me.anno.cache.Promise
import me.anno.maths.Maths.clamp
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.Metadata
import me.anno.remcraft.rendering.v2.invalidateChunk
import me.anno.remcraft.rendering.v2.saveSystem
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsY
import me.anno.remcraft.world.Index.bitsZ
import me.anno.remcraft.world.Index.getIndex
import me.anno.remcraft.world.Index.maskX
import me.anno.remcraft.world.Index.maskY
import me.anno.remcraft.world.Index.maskZ
import me.anno.remcraft.world.decorator.Decorator
import me.anno.utils.pooling.ObjectPool
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4i

class Dimension(val stages: List<Decorator>) {

    val gravity = Vector3f(0f, -9.81f, 0f)

    var timeoutMillis = 5000L

    private val chunks = CacheSection<Vector4i, Chunk>("Chunks")
    private val generatorImpl = { key: Vector4i, result: Promise<Chunk> ->
        if (key.w > 0) {
            // load previous stage, then decorate
            getChunk(key.x, key.y, key.z, key.w - 1).waitFor { prevChunk ->
                val chunk = chunkPool.create()
                chunk.set(key.x, key.y, key.z, key.w - 1)
                prevChunk!!.copyInto(chunk)
                stages[key.w].decorate(chunk)
                chunk.stage = key.w
                result.value = chunk
            }
        } else {
            val chunk = chunkPool.create()
            chunk.set(key.x, key.y, key.z, key.w - 1)
            chunk.clear()
            stages[key.w].decorate(chunk)
            chunk.stage = key.w
            result.value = chunk
        }
    }

    fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int, stageId: Int): Promise<Chunk> {
        if (stageId < 0) throw IllegalArgumentException("Invalid StageID $stageId")
        val stageId = clamp(stageId, 0, stages.size - 1)
        val key = Vector4i(chunkX, chunkY, chunkZ, stageId)
        return chunks.getEntry(key, timeoutMillis, generatorImpl)
    }

    fun getChunkOrNull(chunkX: Int, chunkY: Int, chunkZ: Int, stageId: Int): Promise<Chunk>? {
        if (stageId < 0) throw IllegalArgumentException("Invalid StageID $stageId")
        val stageId = clamp(stageId, 0, stages.size - 1)
        val key = Vector4i(chunkX, chunkY, chunkZ, stageId)
        return chunks.getEntryWithoutGenerator(key)
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int): BlockType? {
        return getBlockAt(globalX, globalY, globalZ, Int.MAX_VALUE)
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int, side: BlockSide): BlockType? {
        return getBlockAt(globalX + side.x, globalY + side.y, globalZ + side.z)
    }

    fun getBlockAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): BlockType? {
        return getChunkAt(globalX, globalY, globalZ, stage)
            ?.getBlock(globalX, globalY, globalZ)
    }

    fun setBlockAt(
        globalX: Int, globalY: Int, globalZ: Int,
        blockType: BlockType, metadata: Metadata?
    ): Boolean {
        val chunk = getChunkAt(globalX, globalY, globalZ, Int.MAX_VALUE) ?: return false
        return if (chunk.setBlock(globalX, globalY, globalZ, blockType, metadata)) {
            chunk.afterBlockChange(globalX, globalY, globalZ)
            true
        } else false
    }

    fun getMetadataAt(globalX: Int, globalY: Int, globalZ: Int): Metadata? =
        getMetadataAt(globalX, globalY, globalZ, Int.MAX_VALUE)

    fun getOrCreateMetadataAt(globalX: Int, globalY: Int, globalZ: Int): Metadata =
        getOrCreateMetadataAt(globalX, globalY, globalZ, Int.MAX_VALUE)

    fun getMetadataAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): Metadata? {
        return getChunkAt(globalX, globalY, globalZ, stage)
            ?.getMetadata(globalX, globalY, globalZ)
    }

    fun getOrCreateMetadataAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int): Metadata {
        return getChunkAt(globalX, globalY, globalZ, stage)!!.getOrCreateMetadata(globalX, globalY, globalZ)
    }

    fun getChunkAt(globalX: Int, globalY: Int, globalZ: Int, stage: Int = Int.MAX_VALUE): Chunk? =
        getChunk(globalX shr bitsX, globalY shr bitsY, globalZ shr bitsZ, stage).waitFor()

    fun unload(chunk: Chunk) {
        for (stageId in 0 until stages.size) {
            // todo add chunks to pool...
            chunks.getEntryWithoutGenerator(Vector4i(chunk.xi, chunk.yi, chunk.zi, stageId))
                ?.destroy()
        }
    }

    fun invalidateAt(x: Int, y: Int, z: Int, newBlock: BlockType) {
        val chunkId = coordsToChunkId(x, y, z)
        // todo chunk invalidation is extremely slow
        // todo when setting blocks, we can temporarily place a block until the mesh has been recalculated
        invalidateChunk(chunkId)
        val localCoords = Vector3i(x and maskX, y and maskY, z and maskZ)
        // when we're on the edge, and we remove a block (set a transparent one), we need to invalidate our neighbors, too
        if (!newBlock.isSolid) {
            if (localCoords.x == 0) invalidateChunk(Vector3i(chunkId).sub(1, 0, 0))
            else if (localCoords.x == maskX) invalidateChunk(Vector3i(chunkId).add(1, 0, 0))
            if (localCoords.y == 0) invalidateChunk(Vector3i(chunkId).sub(0, 1, 0))
            else if (localCoords.y == maskY) invalidateChunk(Vector3i(chunkId).add(0, 1, 0))
            if (localCoords.z == 0) invalidateChunk(Vector3i(chunkId).sub(0, 0, 1))
            else if (localCoords.z == maskZ) invalidateChunk(Vector3i(chunkId).add(0, 0, 1))
        }
        saveSystem.get(chunkId) { changesInChunk ->
            changesInChunk[getIndex(x, y, z)] = newBlock.id
            saveSystem.put(chunkId, changesInChunk)
        }
    }

    fun coordsToChunkId(x: Int, y: Int, z: Int): Vector3i {
        return Vector3i(x shr bitsX, y shr bitsY, z shr bitsZ)
    }

    fun destroy() {
        chunks.clear()
    }

    private val chunkPool = ObjectPool { Chunk(this, 0, 0, 0) }

}