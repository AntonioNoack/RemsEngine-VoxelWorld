package me.anno.minecraft.world

import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.Streams.read0String
import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readBE32
import me.anno.io.Streams.write0String
import me.anno.io.Streams.writeBE16
import me.anno.io.Streams.writeBE32
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.UnknownBlock
import me.anno.utils.OS
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3i
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SaveLoadSystem(name: String) {

    val db = HierarchicalDatabase(
        "blocks",
        OS.documents.getChild("RemsEngine/Tests/$name"),
        10_000_000,
        30_000L, 0L
    )

    val hash = 0L

    fun getPath(chunkId: Vector3i): List<String> {
        return listOf("${chunkId.x},${chunkId.y},${chunkId.z}")
    }

    fun get(chunkId: Vector3i, callback: (HashMap<Vector3i, Short>) -> Unit) {
        db.get(getPath(chunkId), hash) { slice, err ->
            if (slice != null) {
                slice.stream().use { stream ->
                    callback(read(stream))
                }
            } else callback(HashMap())
        }
    }

    private fun read(stream: ByteArrayInputStream): HashMap<Vector3i, Short> {
        // write version ID
        val versionID = stream.readBE16()
        if (versionID != VERSION_1_0) {
            return HashMap()
        }

        // first load all block IDs...
        val numBlockTypes = stream.readBE16().and(0xffff)
        val blockIds = ShortArray(numBlockTypes) {
            val blockType = BlockRegistry.byUUID[stream.read0String()] ?: UnknownBlock
            blockType.id
        }

        // then load all blocks
        val numBlocks = stream.readBE32()
        val answer = HashMap<Vector3i, Short>(numBlocks)
        for (i in 0 until numBlocks) {
            val x = stream.read()
            val y = stream.read()
            val z = stream.read()
            val b = blockIds[stream.readBE16()]
            answer[Vector3i(x, y, z)] = b
        }

        return answer
    }

    fun put(chunkId: Vector3i, blocks: Map<Vector3i, Short>) {
        val bytes = write(blocks)
        db.put(getPath(chunkId), hash, ByteSlice(bytes))
    }

    private fun write(blocks: Map<Vector3i, Short>): ByteArray {

        val typeUUIDs = blocks.values.map { id ->
            BlockRegistry.byId(id) ?: BlockRegistry.Unknown
        }.distinctBy { it.typeUUID }

        val sizeGuess = blocks.size * 5 + 2 + 2 + 4 +
                typeUUIDs.sumOf { it.typeUUID.length + 1 }
        val stream = ByteArrayOutputStream(sizeGuess)

        stream.writeBE16(VERSION_1_0)

        stream.writeBE16(typeUUIDs.size)
        for (i in typeUUIDs.indices) {
            stream.write0String(typeUUIDs[i].typeUUID)
        }
        val idMapping = typeUUIDs.indices
            .associateBy { idx -> typeUUIDs[idx].id }

        stream.writeBE32(blocks.size)
        for ((k, v) in blocks) {
            stream.write(k.x)
            stream.write(k.y)
            stream.write(k.z)
            stream.writeBE16(idMapping[v]!!)
        }

        assertEquals(sizeGuess, stream.size())
        return stream.toByteArray()
    }

    // todo write entities
    // todo write block metadata

    companion object {
        private const val VERSION_1_0 = 0x1000

    }
}