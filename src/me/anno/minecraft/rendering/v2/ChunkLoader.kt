package me.anno.minecraft.rendering.v2

import me.anno.minecraft.rendering.v3.ChunkLoader2.Companion.fluidFilter
import me.anno.minecraft.rendering.v3.ChunkLoader2.Companion.solidFilter
import me.anno.minecraft.world.Dimension
import org.joml.Vector3i

class ChunkLoader(
    solidRenderer: ChunkRenderer,
    fluidRenderer: ChunkRenderer,
    val detailRenderer: DetailChunkRenderer
) : ChunkLoaderBase<ChunkRenderer>(
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
        val model = ChunkLoaderModel(chunk)
        val solidMesh = model.createMesh(palette, solidFilter)
        val fluidMesh = model.createMesh(palette, fluidFilter)
        val detailMesh = createDetailMesh(chunk)

        // clock.stop("CreateMesh")
        dimension.destroy()

        meshUpload(solidRenderer, chunkId, solidMesh, true)
        meshUpload(fluidRenderer, chunkId, fluidMesh, true)
        if (detailMesh != null) {
            meshUpload(detailRenderer, chunkId, detailMesh, false)
        }
    }

    override fun destroyMesh(renderer: ChunkRenderer, vec: Vector3i, destroyMesh: Boolean) {
        renderer.remove(vec, true)
    }
}