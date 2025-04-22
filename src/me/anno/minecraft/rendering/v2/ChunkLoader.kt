package me.anno.minecraft.rendering.v2

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.maths.patterns.SpiralPattern.spiral3d
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.rendering.v1.VisualDimension.Companion.chunkGenQueue
import me.anno.minecraft.world.Chunk
import me.anno.minecraft.world.Dimension
import org.joml.AABBd
import org.joml.Vector3i

class ChunkLoader(
    val solidRenderer: ChunkRenderer,
    val fluidRenderer: ChunkRenderer,
) : Component(), OnUpdate {

    companion object {
        fun mapPalette(mapping: (BlockType) -> Int): IntArray {
            val size = BlockType.byId.keys.max().toInt() + 1
            val palette = IntArray(size)
            for ((id, block) in BlockType.byId) {
                palette[id.toInt().and(0xffff)] = mapping(block)
            }
            return palette
        }
    }

    val worker = chunkGenQueue

    // load world in spiral pattern
    val loadingRadius = 3
    val spiralPattern = spiral3d(loadingRadius + 5, false)
    val loadingPattern = spiralPattern.filter { it.length() < loadingRadius - 0.5f }
    val unloadingPattern = spiralPattern.filter { it.length() > loadingRadius + 1.5f }

    val loadedChunks = HashSet<Vector3i>()

    val palette = if (solidRenderer.material is TextureMaterial) {
        mapPalette { it.texId + 1 }
    } else {
        mapPalette { it.color }
    }

    fun generateChunk(chunkId: Vector3i) {
        // val clock = Clock("ChunkLoader")
        // 9s vs 27s, so 3x faster to use a clone 🤯
        // todo fix that... we cannot be THAT slow just to synchronize stuff...
        val dimension = Dimension(dimension.generator, dimension.decorators)
        val chunk = dimension.getChunk(chunkId.x, chunkId.y, chunkId.z, Int.MAX_VALUE)!!
        val solidMesh = createMesh(chunk) { a, b -> a.isSolid && !b.isSolid }
        val fluidMesh = createMesh(chunk) { a, b -> a.isFluid && b == BlockType.Air }

        // clock.stop("CreateMesh")
        dimension.destroy()

        meshUpload(solidRenderer, chunkId, solidMesh)
        meshUpload(fluidRenderer, chunkId, fluidMesh)
    }

    private fun createMesh(chunk: Chunk, blockFilter: (BlockType, BlockType) -> Boolean): Mesh {
        val model = ChunkLoaderModel(chunk)
        model.center0()
        return model.createMesh(palette, { x, y, z ->
            dimension.getBlockAt(
                x + chunk.x0,
                y + chunk.y0,
                z + chunk.z0
            ).id.toInt().and(0xffff)
        }, { inside, outside ->
            val inside1 = BlockType.byId[inside.toShort()]!!
            val outside1 = BlockType.byId[outside.toShort()]!!
            blockFilter(inside1, outside1)
        })
    }

    private fun meshUpload(renderer: ChunkRenderer, chunkId: Vector3i, mesh: Mesh) {
        val data = renderer.getData(chunkId, mesh)
        if (data != null) {
            val bounds = AABBd(mesh.getBounds())
            val x0 = chunkId.x * csx
            val y0 = chunkId.y * csy
            val z0 = chunkId.z * csz
            bounds.translate(x0.toDouble(), y0.toDouble(), z0.toDouble())
            addGPUTask("ChunkUpload", 1) { // change back to GPU thread
                renderer.set(chunkId, MeshEntry(mesh, bounds, data))
            }
        }
    }

    fun AABBd.translate(dx: Double, dy: Double, dz: Double) {
        minX += dx
        minY += dy
        minZ += dz
        maxX += dx
        maxY += dy
        maxZ += dz
    }

    val workerLimit = worker.numThreads * 2 + 1
    fun loadChunks(center: Vector3i) {
        if (worker.size >= workerLimit) return
        for (idx in loadingPattern) {
            val vec = Vector3i(idx).add(center)
            if (loadedChunks.add(vec)) {
                worker += { generateChunk(vec) }
                if (worker.size >= workerLimit) return
            }
        }
    }

    fun unloadChunks(center: Vector3i) {
        for (idx in unloadingPattern) {
            val vec = Vector3i(idx).add(center)
            if (loadedChunks.remove(vec)) {
                solidRenderer.remove(vec, true)
                fluidRenderer.remove(vec, true)
            }
        }
    }

    fun getPlayerChunkId(): Vector3i {
        val delta = Vector3i()
        val ci = RenderView.currentInstance
        if (ci != null) {
            val pos = ci.orbitCenter // around where the camera orbits
            delta.set((pos.x / csx).toInt(), 0, (pos.z / csz).toInt())
        }
        return delta
    }

    override fun onUpdate() {
        // load next mesh
        if (worker.remaining <= worker.numThreads) {
            val chunkId = getPlayerChunkId()
            loadChunks(chunkId)
            unloadChunks(chunkId)
        }
    }
}