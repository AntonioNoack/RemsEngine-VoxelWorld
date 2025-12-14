package me.anno.minecraft.rendering.v3

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.minecraft.rendering.v2.ChunkLoaderBase
import me.anno.minecraft.rendering.v2.DetailChunkRenderer
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.world.Dimension
import org.joml.Vector3i

class ChunkLoader2(
    solidRenderer: ChunkRenderer2,
    fluidRenderer: ChunkRenderer2,
    val detailRenderer: DetailChunkRenderer
) : ChunkLoaderBase<ChunkRenderer2>(
    solidRenderer.material,
    solidRenderer,
    fluidRenderer
) {
    override fun generateChunk(chunkId: Vector3i) {
        // val clock = Clock("ChunkLoader")
        // 9s vs 27s, so 3x faster to use a clone 🤯
        // todo fix that... we cannot be THAT slow just to synchronize stuff...
        val dimension = Dimension(dimension.generator, dimension.decorators)
        val chunk = dimension.getChunk(chunkId.x, chunkId.y, chunkId.z, Int.MAX_VALUE)!!
        // val model = ChunkLoaderModel(chunk)
        val solidMesh = ChunkMesh(chunk, solidRenderer, solidFilter) // model.createMesh(palette, solidFilter)
        val fluidMesh = ChunkMesh(chunk, solidRenderer, fluidFilter) // model.createMesh(palette, fluidFilter)
        val detailMesh = createDetailMesh(chunk)

        // clock.stop("CreateMesh")
        dimension.destroy()

        meshUpload(solidRenderer, chunkId, solidMesh)
        meshUpload(fluidRenderer, chunkId, fluidMesh)
        if (detailMesh != null) {
            meshUpload(detailRenderer, chunkId, detailMesh, false)
        }
    }

    private fun meshUpload(renderer: ChunkRenderer2, chunkId: Vector3i, mesh: ChunkMesh) {
        renderer.remove(chunkId, destroyMesh = true)
        addGPUTask("ChunkUpload", 1) { // change back to GPU thread
            renderer.add(chunkId, mesh)
        }
    }

    override fun destroyMesh(renderer: ChunkRenderer2, vec: Vector3i, destroyMesh: Boolean) {
        renderer.remove(vec, true)
    }
}