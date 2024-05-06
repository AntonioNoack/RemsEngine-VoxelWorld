package me.anno.minecraft.world

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.Entity

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Chunk(val dim: Dimension, x0: Int, y0: Int, z0: Int) : Saveable() {

    var x0 = x0
        private set
    var y0 = y0
        private set
    var z0 = z0
        private set

    var chunkX = x0 shr dim.bitsX
        private set
    var chunkY = y0 shr dim.bitsY
        private set
    var chunkZ = z0 shr dim.bitsZ
        private set

    var x1 = x0 + dim.sizeX
        private set
    var y1 = y0 + dim.sizeY
        private set
    var z1 = z0 + dim.sizeZ
        private set

    var decorator = 0

    // what is the maximum number of block types? int32, I think
    // int16 will be enough without mods
    // probably still is good enough with metadata :)
    val blocks = ShortArray(dim.totalSize)
    val metadata = HashMap<Int, Metadata>()

    val entities = ArrayList<Entity>()

    fun set(x: Int, y: Int, z: Int) {
        x0 = x
        y0 = y
        z0 = z
        chunkX = x0 shr dim.bitsX
        chunkY = y0 shr dim.bitsY
        chunkZ = z0 shr dim.bitsZ
        x1 = x0 + dim.sizeX
        y1 = y0 + dim.sizeY
        z1 = z0 + dim.sizeZ
    }

    fun clear() {
        blocks.fill(0)
        metadata.clear()
        entities.clear()
        decorator = 0
    }

    fun getIndex(localX: Int, localY: Int, localZ: Int): Int {
        return dim.getIndex(localX, localY, localZ)
    }

    fun isAir(index: Int): Boolean {
        return blocks[index] == 0.toShort()
    }

    fun isAir(localX: Int, localY: Int, localZ: Int): Boolean {
        return blocks[dim.getIndex(localX, localY, localZ)] == 0.toShort()
    }

    fun getBlockId(localX: Int, localY: Int, localZ: Int): Short {
        return blocks[dim.getIndex(localX, localY, localZ)]
    }

    fun getBlock(localX: Int, localY: Int, localZ: Int): BlockType {
        return BlockType.byId[blocks[dim.getIndex(localX, localY, localZ)]] ?: Air
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Short): Boolean {
        val key = dim.getIndex(x, y, z)
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
        val dim = dim
        return if (lx in 0 until dim.sizeX && ly in 0 until dim.sizeY && lz in 0 until dim.sizeZ) {
            setBlock(lx, ly, lz, block)
        } else false
    }

    fun addBlockWithin(lx: Int, ly: Int, lz: Int, block: BlockType): Boolean {
        val dim = dim
        return if (lx in 0 until dim.sizeX && ly in 0 until dim.sizeY && lz in 0 until dim.sizeZ) {
            val index = dim.getIndex(lx, ly, lz)
            if (blocks[index] == 0.toShort()) {
                blocks[index] = block.id
                true
            } else false
        } else false
    }

    fun setBlockQuickly(x: Int, y: Int, z: Int, block: Short) {
        blocks[dim.getIndex(x, y, z)] = block
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