package me.anno.remcraft.world

import me.anno.cache.ICacheData
import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.Metadata
import me.anno.remcraft.block.types.ChangingBlock
import me.anno.remcraft.entity.RemcraftEntity
import me.anno.remcraft.rendering.v2.chunkLoader
import me.anno.remcraft.world.Dimension.Companion.chunkPool
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsY
import me.anno.remcraft.world.Index.bitsZ
import me.anno.remcraft.world.Index.getIndex
import me.anno.remcraft.world.Index.indexToX
import me.anno.remcraft.world.Index.indexToY
import me.anno.remcraft.world.Index.indexToZ
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.Index.totalSize
import me.anno.ui.UIColors
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBd
import org.joml.Vector3i
import speiger.primitivecollections.IntToObjectHashMap

class Chunk : Saveable(), ICacheData {

    companion object {
        var numCreated = 0
        var numRecycled = 0
        var numUsed = 0
    }

    var x0 = 0
        private set
    var y0 = 0
        private set
    var z0 = 0
        private set

    var xi = 0
        private set
    var yi = 0
        private set
    var zi = 0
        private set

    var x1 = sizeX
        private set
    var y1 = sizeY
        private set
    var z1 = sizeZ
        private set

    lateinit var dimension: Dimension

    var stage = 0

    // what is the maximum number of block types? int32, I think
    // int16 will be enough without mods
    // probably still is good enough with metadata :)
    val blocks = ShortArray(totalSize)
    val metadata = IntToObjectHashMap<Metadata>()

    val entities = ArrayList<RemcraftEntity>()
    val blockUpdates = IntArrayList()

    fun afterBlockChange(x: Int, y: Int, z: Int) {
        val index = getIndex(x, y, z)
        blockUpdates.add(index)
        dimension.invalidateAt(x, y, z, getBlock(index))
    }

    fun afterBlockChangeI(x: Int, y: Int, z: Int) {
        afterBlockChange(x, y, z)

        val bounds = AABBd(
            x - 0.05, y - 0.05, z - 0.05,
            x + 1.05, y + 1.05, z + 1.05
        )
        DebugShapes.showDebugAABB(DebugAABB(bounds, UIColors.fireBrick, 0.1f))
    }

    fun processBlockUpdates() {
        val size0 = blockUpdates.size
        for (i in blockUpdates.indices) {
            processBlockUpdate(blockUpdates[i])
        }
        blockUpdates.removeBetween(0, size0)
    }

    /**
     * update center and all sides
     * */
    fun processBlockUpdate(i: Int) {
        val x = indexToX(i) + x0
        val y = indexToY(i) + y0
        val z = indexToZ(i) + z0

        processBlockUpdate(x, y, z)
        processBlockUpdate(x - 1, y, z)
        processBlockUpdate(x + 1, y, z)
        processBlockUpdate(x, y - 1, z)
        processBlockUpdate(x, y + 1, z)
        processBlockUpdate(x, y, z - 1)
        processBlockUpdate(x, y, z + 1)
    }

    fun processBlockUpdate(x: Int, y: Int, z: Int) {
        val chunk = dimension.getChunkAt(x, y, z) ?: return
        val index = getIndex(x, y, z)
        val type = chunk.getBlock(index) as? ChangingBlock ?: return
        type.onBlockUpdate(x, y, z, chunk.getMetadata(index), chunk)
    }

    fun set(dim: Dimension, xi: Int, yi: Int, zi: Int, stage: Int) {
        if (++numUsed % 1000 == 0) {
            try {
                println(
                    "Chunk stats: $numCreated created, $numUsed used, $numRecycled destroyed, " +
                            "($xi,$yi,$zi) $stage), ${chunkLoader.loadedChunks.size} chunks loaded, ${chunkLoader.loadingPattern.size} pattern size"
                )
            } catch (e: Exception) {
            }
        }
        dimension = dim
        x0 = xi shl bitsX
        y0 = yi shl bitsY
        z0 = zi shl bitsZ
        this.xi = xi
        this.yi = yi
        this.zi = zi
        x1 = x0 + sizeX
        y1 = y0 + sizeY
        z1 = z0 + sizeZ
        this.stage = stage
    }

    fun clear() {
        blocks.fill(0)
        metadata.clear()
        entities.clear()
    }

    fun copyInto(chunk: Chunk) {
        check(chunk !== this)
        blocks.copyInto(chunk.blocks)
        chunk.metadata.clear()
        chunk.metadata.addAll(metadata)
        chunk.entities.clear()
        chunk.entities.addAll(entities)
    }

    fun getBlockId(localX: Int, localY: Int, localZ: Int): Short {
        return blocks[getIndex(localX, localY, localZ)]
    }

    fun getBlock(localX: Int, localY: Int, localZ: Int): BlockType {
        return getBlock(getIndex(localX, localY, localZ))
    }

    fun getBlock(index: Int): BlockType {
        return BlockRegistry.byId(blocks[index]) ?: BlockRegistry.Air
    }

    fun getMetadata(localX: Int, localY: Int, localZ: Int): Metadata? {
        return getMetadata(getIndex(localX, localY, localZ))
    }

    fun getOrCreateMetadata(localX: Int, localY: Int, localZ: Int): Metadata {
        return getOrCreateMetadata(getIndex(localX, localY, localZ))
    }

    fun getMetadata(index: Int): Metadata? = metadata[index]

    fun getOrCreateMetadata(index: Int): Metadata = metadata.getOrPut(index, ::Metadata)

    fun setBlock(x: Int, y: Int, z: Int, block: Short): Boolean {
        return setBlock(getIndex(x, y, z), block)
    }

    fun setBlock(index: Int, block: Short): Boolean {
        var changed = metadata.remove(index) == null
        if (!changed && blocks[index] != block) {
            changed = true
        }
        blocks[index] = block
        return changed
    }

    fun setBlock(x: Int, y: Int, z: Int, block: BlockType): Boolean {
        return setBlock(x, y, z, block.id)
    }

    fun setBlock(v: Vector3i, block: BlockType, metadata: Metadata?): Boolean {
        return setBlock(v.x, v.y, v.z, block, metadata)
    }

    fun setBlock(x: Int, y: Int, z: Int, block: BlockType, metadata: Metadata?): Boolean {
        val index = getIndex(x, y, z)
        val changed0 = setBlock(x, y, z, block.id)
        val oldMetadata = this.metadata[index]
        if (metadata != null) this.metadata[index] = metadata
        else this.metadata.remove(index)
        val changed1 = metadata != oldMetadata
        return changed0 || changed1
    }

    fun setBlockIfAir(lx: Int, ly: Int, lz: Int, block: BlockType): Boolean {
        return if (lx in 0 until sizeX && ly in 0 until sizeY && lz in 0 until sizeZ) {
            val index = getIndex(lx, ly, lz)
            if (blocks[index] == 0.toShort()) {
                blocks[index] = block.id
                true
            } else false
        } else false
    }

    fun setBlockQuickly(x: Int, y: Int, z: Int, block: Short) {
        blocks[getIndex(x, y, z)] = block
    }

    fun setBlockQuickly(x: Int, y: Int, z: Int, block: BlockType) {
        setBlockQuickly(x, y, z, block.id)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "entities", entities)
        writer.writeInt("decorator", stage)
        metadata.forEach { key, value ->
            writer.writeObject(this, "m$key", value)
        }
        writer.writeShortArray("blocks", blocks)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "entities" -> {
                val values = (value as? List<*>) ?: return
                entities.clear()
                entities.addAll(values.filterIsInstance<RemcraftEntity>())
            }
            "blocks" -> (value as? ShortArray)?.copyInto(blocks)
            else -> {
                if (name.startsWith("m") && value is Metadata) {
                    val id = name.substring(1).toIntOrNull()
                    if (id != null) {
                        metadata[id] = value
                    }
                } else super.setProperty(name, value)
            }
        }
    }

    fun getBlockAround(lx: Int, ly: Int, lz: Int): BlockType? {
        return if (lx in 0 until sizeX && ly in 0 until sizeY && lz in 0 until sizeZ) {
            getBlock(getIndex(lx, ly, lz))
        } else {
            dimension.getBlockAt(x0 + lx, y0 + ly, z0 + lz)
        }
    }

    override val className = "Chunk"

    init {
        numCreated++
    }

    override fun destroy() {
        chunkPool.destroy(this)
        numRecycled++
    }

}