package me.anno.minecraft.rendering.v1

import me.anno.ecs.components.mesh.Mesh
import me.anno.minecraft.block.BlockType
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
        val dimension = chunk.dimension
        ChunkVoxelModel(chunk)
            .createMesh(palette, { x, y, z ->
                !chunk.getBlock(x, y, z).isTransparent
            }, { x, y, z ->
                !dimension.getChunkAt(x + chunk.x0, y + chunk.y0, z + chunk.z0, true)!!
                    .getBlock(
                        x and dimension.maskX,
                        y and dimension.maskY,
                        z and dimension.maskZ
                    ).isTransparent
            }, mesh)
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