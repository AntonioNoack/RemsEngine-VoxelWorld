package me.anno.minecraft.visual

import me.anno.ecs.components.mesh.Mesh
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.world.Chunk
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager

class VisualChunk {

    // todo sort triangles by side normal, back 2 front,
    // this will allow us to sort all water faces just by sorting the chunks

    var chunk: Chunk? = null

    var wasSeen = true
    var hasMesh = false

    val mesh = Mesh()

    fun generateMesh() {
        // todo handle transparent blocks slightly differently
        val chunk = chunk ?: return
        val t0 = System.nanoTime()
        ChunkVoxelModel(chunk)
            .createMesh(palette, null, null, mesh)
        val t1 = System.nanoTime()
        if (printTimes) LOGGER.info("mesh ${((t1 - t0) * 1e-6).f3()}ms/c")
        hasMesh = true
    }

    fun destroy() {
        mesh.destroy()
    }

    companion object {

        private val LOGGER = LogManager.getLogger(VisualChunk::class)
        var printTimes = false

        val palette by lazy {
            val palette = IntArray(BlockType.library.maxOf { it.id } + 1)
            for (block in BlockType.library) {
                palette[block.id.toInt()] = block.color
            }
            palette
        }
    }
}