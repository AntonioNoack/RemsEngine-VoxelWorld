package me.anno.minecraft.rendering.v2

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.maths.patterns.SpiralPattern.spiral3d
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.DetailedBlockVisuals
import me.anno.minecraft.block.builder.DetailedBlockMesh32
import me.anno.minecraft.rendering.v1.VisualDimension.Companion.chunkGenQueue
import me.anno.minecraft.world.Chunk
import me.anno.minecraft.world.Dimension
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBd
import org.joml.Vector3i

class ChunkLoader(
    val solidRenderer: ChunkRenderer,
    val fluidRenderer: ChunkRenderer,
    val detailChunkRenderer: DetailChunkRenderer
) : Component(), OnUpdate {

    companion object {
        fun mapPalette(mapping: (BlockType) -> Int): IntArray {
            val byId = BlockRegistry.byId
            return IntArray(byId.size) { id ->
                mapping(byId[id])
            }
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
        val solidMesh = createMesh(chunk) { a, b -> isSolid(a) && !isSolid(b) }
        val fluidMesh = createMesh(chunk) { a, b -> a.isFluid && b == BlockRegistry.Air }
        val detailMesh = createDetailMesh(chunk)

        // clock.stop("CreateMesh")
        dimension.destroy()

        meshUpload(solidRenderer, chunkId, solidMesh)
        meshUpload(fluidRenderer, chunkId, fluidMesh)
        if (detailMesh != null) {
            meshUpload2(detailChunkRenderer, chunkId, detailMesh)
        }
    }

    private fun isSolid(a: BlockType): Boolean {
        return a.isSolid && a !is DetailedBlockVisuals
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
            val inside1 = BlockRegistry.byId[inside]
            val outside1 = BlockRegistry.byId[outside]
            blockFilter(inside1, outside1)
        })
    }

    private fun createDetailMesh(chunk: Chunk): DetailedBlockMesh32? {
        val joinedData = IntArrayList(64)
        for (z in 0 until dimension.sizeZ) {
            for (y in 0 until dimension.sizeY) {
                for (x in 0 until dimension.sizeX) {
                    val block = chunk.getBlock(x, y, z)
                    if (block.isSolid && block is DetailedBlockVisuals) {
                        val dx = (chunk.x0 + x) * 16
                        val dy = (chunk.y0 + y) * 16
                        val dz = (chunk.z0 + z) * 16
                        val blockMesh = block.getModel().data!!
                        joinedData.ensureExtra(blockMesh.size)
                        forLoopSafely(blockMesh.size, 4) { i ->
                            joinedData.add(blockMesh[i] + dx)
                            joinedData.add(blockMesh[i + 1] + dy)
                            joinedData.add(blockMesh[i + 2] + dz)
                            joinedData.add(blockMesh[i + 3].toInt().and(0xffff))
                        }
                    }
                }
            }
        }
        if (joinedData.isEmpty()) return null
        val mesh = DetailedBlockMesh32()
        mesh.data = joinedData.toIntArray()
        return mesh
    }

    private fun <V : IMesh> meshUpload(renderer: UniqueMeshRenderer<V, Vector3i>, chunkId: Vector3i, mesh: V) {
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
        } else renderer.remove(chunkId, deleteMesh = true)
    }

    private fun <V : IMesh> meshUpload2(renderer: UniqueMeshRenderer<V, Vector3i>, chunkId: Vector3i, mesh: V) {
        val data = renderer.getData(chunkId, mesh)
        if (data != null) {
            addGPUTask("ChunkUpload", 1) { // change back to GPU thread
                renderer.set(chunkId, MeshEntry(mesh, AABBd(mesh.getBounds()), data))
            }
        } else renderer.remove(chunkId, deleteMesh = true)
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