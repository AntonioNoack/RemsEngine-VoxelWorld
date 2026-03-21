package me.anno.remcraft.rendering.v2

import me.anno.remcraft.world.Chunk
import org.joml.Vector3i

class ChunkLoader(
    solidRenderer: ChunkRenderer,
    fluidRenderer: ChunkRenderer,
    val detailRenderer: DetailChunkRenderer
) : ChunkLoaderBase<ChunkRenderer>(
    solidRenderer.material,
    solidRenderer, fluidRenderer
) {

    override fun generateChunk(chunkId: Vector3i) {
        dimension.getChunk(chunkId.x, chunkId.y, chunkId.z, Int.MAX_VALUE)
            .waitFor { chunk -> if (chunk != null) generateChunkMeshes(chunkId, chunk) }
    }

    private fun generateChunkMeshes(chunkId: Vector3i, chunk: Chunk) {
        // val clock = me.anno.utils.Clock("ChunkLoader")
        val model = ChunkLoaderModel(chunk)
        val solidMesh = model.createMesh(palette, solidFilter)
        val fluidMesh = model.createMesh(palette, fluidFilter)
        val detailMesh = createDetailMesh(chunk)

        // clock.stop("CreateMesh")

        meshUpload(solidRenderer, chunkId, solidMesh, true)
        meshUpload(fluidRenderer, chunkId, fluidMesh, true)
        if (detailMesh != null) {
            meshUpload(detailRenderer, chunkId, detailMesh, false)
        }
    }

    override fun destroyMesh(renderer: ChunkRenderer, vec: Vector3i, destroyMesh: Boolean) {
        synchronized(renderer) {
            renderer.remove(vec, true)
        }
    }
}