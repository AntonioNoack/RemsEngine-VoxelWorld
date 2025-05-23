package me.anno.minecraft.world

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.Entity

class Chunk(val dimension: Dimension, x0: Int, y0: Int, z0: Int) : Saveable() {

    var x0 = x0
        private set
    var y0 = y0
        private set
    var z0 = z0
        private set

    var chunkX = x0 shr dimension.bitsX
        private set
    var chunkY = y0 shr dimension.bitsY
        private set
    var chunkZ = z0 shr dimension.bitsZ
        private set

    var x1 = x0 + dimension.sizeX
        private set
    var y1 = y0 + dimension.sizeY
        private set
    var z1 = z0 + dimension.sizeZ
        private set

    var decorator = 0

    // what is the maximum number of block types? int32, I think
    // int16 will be enough without mods
    // probably still is good enough with metadata :)
    val blocks = ShortArray(dimension.totalSize)
    val metadata = HashMap<Int, Metadata>()

    val entities = ArrayList<Entity>()

    fun set(x: Int, y: Int, z: Int) {
        x0 = x
        y0 = y
        z0 = z
        chunkX = x0 shr dimension.bitsX
        chunkY = y0 shr dimension.bitsY
        chunkZ = z0 shr dimension.bitsZ
        x1 = x0 + dimension.sizeX
        y1 = y0 + dimension.sizeY
        z1 = z0 + dimension.sizeZ
    }

    fun clear() {
        blocks.fill(0)
        metadata.clear()
        entities.clear()
        decorator = 0
    }

    fun getIndex(localX: Int, localY: Int, localZ: Int): Int {
        return dimension.getIndex(localX, localY, localZ)
    }

    fun isAir(index: Int): Boolean {
        return blocks[index] == 0.toShort()
    }

    fun isAir(localX: Int, localY: Int, localZ: Int): Boolean {
        return blocks[dimension.getIndex(localX, localY, localZ)] == 0.toShort()
    }

    fun getBlockId(localX: Int, localY: Int, localZ: Int): Short {
        return blocks[dimension.getIndex(localX, localY, localZ)]
    }

    fun getBlock(localX: Int, localY: Int, localZ: Int): BlockType {
        return BlockRegistry.byId(blocks[dimension.getIndex(localX, localY, localZ)]) ?: BlockRegistry.Air
    }

    fun getMetadata(localX: Int, localY: Int, localZ: Int): Metadata? {
        return metadata[dimension.getIndex(localX, localY, localZ)]
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Short): Boolean {
        val key = dimension.getIndex(x, y, z)
        var changed = metadata.remove(key) == null
        if (!changed && blocks[key] != block) {
            changed = true
        }
        blocks[key] = block
        return changed
    }

    fun setBlock(x: Int, y: Int, z: Int, block: BlockType): Boolean {
        return setBlock(x, y, z, block.id)
    }

    fun setBlockWithin(lx: Int, ly: Int, lz: Int, block: BlockType): Boolean {
        val dim = dimension
        return if (lx in 0 until dim.sizeX && ly in 0 until dim.sizeY && lz in 0 until dim.sizeZ) {
            setBlock(lx, ly, lz, block)
        } else false
    }

    fun addBlockWithin(lx: Int, ly: Int, lz: Int, block: BlockType): Boolean {
        val dim = dimension
        return if (lx in 0 until dim.sizeX && ly in 0 until dim.sizeY && lz in 0 until dim.sizeZ) {
            val index = dim.getIndex(lx, ly, lz)
            if (blocks[index] == 0.toShort()) {
                blocks[index] = block.id
                true
            } else false
        } else false
    }

    fun setBlockQuickly(x: Int, y: Int, z: Int, block: Short) {
        blocks[dimension.getIndex(x, y, z)] = block
    }

    fun setBlockQuickly(x: Int, y: Int, z: Int, block: BlockType) {
        setBlockQuickly(x, y, z, block.id)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "entities", entities)
        writer.writeInt("decorator", decorator)
        for ((key, value) in metadata) {
            writer.writeObject(this, "m$key", value)
        }
        writer.writeShortArray("blocks", blocks)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "entities" -> {
                val values = (value as? List<*>) ?: return
                entities.clear()
                entities.addAll(values.filterIsInstance<Entity>())
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

    override val className = "Chunk"

}