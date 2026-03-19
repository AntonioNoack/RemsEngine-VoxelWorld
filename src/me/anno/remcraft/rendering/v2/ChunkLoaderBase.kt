package me.anno.remcraft.rendering.v2

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRendererImpl
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.maths.patterns.SpiralPattern.spiral3d
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.types.DetailedBlockVisuals
import me.anno.remcraft.block.builder.DetailedBlockMesh32
import me.anno.remcraft.rendering.v1.VisualDimension.Companion.chunkGenQueue
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.ui.UIColors
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBd
import org.joml.Vector3i
import kotlin.math.floor

abstract class ChunkLoaderBase<ChunkRenderer>(
    solidMaterial: Material,
    val solidRenderer: ChunkRenderer,
    val fluidRenderer: ChunkRenderer
) : Component(), OnUpdate {

    companion object {

        fun mapPalette(mapping: (BlockType) -> Int): IntArray {
            val byId = BlockRegistry.byId
            return IntArray(byId.size) { id ->
                mapping(byId[id])
            }
        }

        fun getPlayerChunkId(): Vector3i {
            val delta = Vector3i()
            val ci = RenderView.currentInstance
            if (ci != null) {
                val pos = ci.orbitCenter // around where the camera orbits
                delta.set(floor(pos.x / sizeX).toInt(), floor(pos.y / sizeY).toInt(), floor(pos.z / sizeZ).toInt())
            }
            return delta
        }

        fun isSolid(a: BlockType): Boolean {
            return a.isSolid && a !is DetailedBlockVisuals
        }

        val solidFilter: BlockFilter = { a, b -> isSolid(a) && !isSolid(b) }
        val fluidFilter: BlockFilter = { a, b -> a.isFluid && b == BlockRegistry.Air }
    }

    val worker = chunkGenQueue

    // load world in spiral pattern
    val loadingRadius = 3
    val spiralPattern = spiral3d(loadingRadius + 5, false)
    val loadingPattern = spiralPattern.filter { it.length() < loadingRadius - 0.5f }
    val unloadingPattern = spiralPattern.filter { it.length() > loadingRadius + 1.5f }

    val loadedChunks = HashSet<Vector3i>()

    val palette = if (solidMaterial is TextureMaterial) {
        mapPalette { it.texId + 1 }
    } else {
        mapPalette { it.color }
    }

    abstract fun generateChunk(chunkId: Vector3i)

    fun createDetailMesh(chunk: Chunk): DetailedBlockMesh32? {
        val joinedData = IntArrayList(64)
        for (z in 0 until sizeZ) {
            for (y in 0 until sizeY) {
                for (x in 0 until sizeX) {
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

    fun <V : IMesh> meshUpload(
        renderer: UniqueMeshRendererImpl<Vector3i, V>, chunkId: Vector3i, mesh: V,
        translate: Boolean,
    ) {
        val bounds = AABBd(mesh.getBounds())
        if (translate) {
            val x0 = chunkId.x * sizeX
            val y0 = chunkId.y * sizeY
            val z0 = chunkId.z * sizeZ
            bounds.translate(x0.toDouble(), y0.toDouble(), z0.toDouble())
        }

        // todo a single click often causes two chunks to change... why???
        // todo there is also the problem, that something only changes one block-change after...
        addGPUTask("ChunkUpload", 1) { // change back to GPU thread
            synchronized(renderer) {
                renderer.remove(chunkId, true)
                renderer.add(chunkId, mesh, bounds)

                val bounds = chunkId.toBounds().addMargin(-0.1)
                DebugShapes.showDebugAABB(DebugAABB(bounds, UIColors.dodgerBlue, 2f))
            }
        }
    }

    val workerLimit = worker.numThreads * 2 + 1
    fun loadChunks(center: Vector3i) {
        if (worker.size >= workerLimit) return
        for (offset in loadingPattern) {
            val chunkId = Vector3i(offset).add(center)
            if (loadedChunks.add(chunkId)) {
                worker += { generateChunk(chunkId) }
                if (worker.size >= workerLimit) return
            }
        }
    }

    abstract fun destroyMesh(renderer: ChunkRenderer, vec: Vector3i, destroyMesh: Boolean)

    fun unloadChunks(center: Vector3i) {
        for (offset in unloadingPattern) {
            val chunkId = Vector3i(offset).add(center)
            if (loadedChunks.remove(chunkId)) {
                destroyMesh(solidRenderer, chunkId, true)
                destroyMesh(fluidRenderer, chunkId, true)
            }
        }
    }

    override fun onUpdate() {
        // load next mesh
        if (worker.remaining <= worker.numThreads) {
            val chunkId = getPlayerChunkId()
            loadChunks(chunkId)
            unloadChunks(chunkId)
        }

        showChunks()
    }

    fun showChunks() {
        for (chunkId in loadedChunks) {
            val isInvalid = chunkId in invalidChunks
            val color = if (isInvalid) UIColors.fireBrick else UIColors.axisWColor
            val bounds = chunkId.toBounds()
            DebugShapes.showDebugAABB(DebugAABB(bounds, color, 0f))
        }
    }

    fun Vector3i.toBounds() = AABBd()
        .setMin(sizeX * (x + 0.0), sizeY * (y + 0.0), sizeZ * (z + 0.0))
        .setMax(sizeX * (x + 1.0), sizeY * (y + 1.0), sizeZ * (z + 1.0))
}