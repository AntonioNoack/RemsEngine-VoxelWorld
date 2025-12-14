package me.anno.minecraft.rendering.v1

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.vox.meshing.GetBlockId
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.rendering.v2.ChunkLoaderBase.Companion.mapPalette
import me.anno.minecraft.world.Chunk
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager

class VisualChunk {

    // todo sort triangles by side normal, back 2 front,
    // this will allow us to sort all water faces just by sorting the chunks

    var chunk: Chunk? = null

    var wasSeen = true
    var hasMesh = false

    val solidMesh = Mesh()
    val fluidMesh = Mesh()

    fun generateMesh() {
        val chunk = chunk ?: return
        val t0 = System.nanoTime()
        val dimension = chunk.dimension
        val outsideBlocks = GetBlockId { x, y, z ->
            dimension.getBlockAt(x + chunk.x0, y + chunk.y0, z + chunk.z0, Int.MAX_VALUE)
                .id.toInt()
        }

        ChunkVoxelModel(chunk)
            .createMesh(palette, outsideBlocks, { insideId, outsideId ->
                val inside = BlockRegistry.byId[insideId]
                val outside = BlockRegistry.byId[outsideId]
                inside.isSolid && !outside.isSolid
            }, solidMesh)

        ChunkVoxelModel(chunk)
            .createMesh(palette, outsideBlocks, { insideId, outsideId ->
                val inside = BlockRegistry.byId[insideId]
                val outside = BlockRegistry.byId[outsideId]
                inside.isFluid && outside == BlockRegistry.Air
            }, fluidMesh)

        val t1 = System.nanoTime()
        if (printTimes) LOGGER.info("mesh ${((t1 - t0) * 1e-6).f3()}ms/c")
        hasMesh = true
    }

    fun destroy() {
        solidMesh.destroy()
        fluidMesh.destroy()
    }

    companion object {

        private val LOGGER = LogManager.getLogger(VisualChunk::class)
        var printTimes = false

        val palette by lazy { mapPalette { it.color } }
    }
}