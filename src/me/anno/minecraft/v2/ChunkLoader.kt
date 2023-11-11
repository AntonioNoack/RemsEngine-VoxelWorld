package me.anno.minecraft.v2

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX.addGPUTask
import me.anno.maths.patterns.SpiralPattern.spiral3d
import me.anno.mesh.vox.model.VoxelModel
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.visual.VisualDimension.Companion.chunkGenQueue
import org.joml.AABBf
import org.joml.Vector3i

class ChunkLoader(val chunkRenderer: ChunkRenderer) : Component() {

    val worker = chunkGenQueue

    // load world in spiral pattern
    val loadingRadius = 10
    val spiralPattern = spiral3d(loadingRadius + 5, false).toList()
    val loadingPattern = spiralPattern.filter { it.length() < loadingRadius - 0.5f }
    val unloadingPattern = spiralPattern.filter { it.length() > loadingRadius + 1.5f }

    val loadedChunks = HashSet<Vector3i>()

    val useRawColors = chunkRenderer.material !is TextureMaterial
    val blockLookup: (BlockType) -> Int = if (useRawColors) {
        { it.color }
    } else {
        {
            when (it) {
                BlockType.Stone -> 48
                BlockType.Grass -> 226
                BlockType.Dirt -> 112
                BlockType.Leaves -> 240
                BlockType.Log -> 189
                BlockType.Sandstone -> 185
                else -> it.id.toInt() - 1
            } + 1
        }
    }

    fun generateChunk(chunkId: Vector3i) {

        val x0 = chunkId.x * csx
        val y0 = chunkId.y * csy
        val z0 = chunkId.z * csz

        val model = object : VoxelModel(csx, csy, csz) {
            override fun getBlock(x: Int, y: Int, z: Int): Int {
                return blockLookup(world.getBlockAt(x0 + x, y0 + y, z0 + z, -1))
            }
        }

        model.centerX = 0f
        model.centerY = 0f
        model.centerZ = 0f

        val mesh = model.createMesh(null, null, { x, y, z ->
            world.getBlockAt(x0 + x, y0 + y, z0 + z, -1) != BlockType.Air
        })

        val data = chunkRenderer.getData(chunkId, mesh)
        if (data != null) {
            val bounds = mesh.getBounds()
            bounds.translate(x0.toFloat(), y0.toFloat(), z0.toFloat())
            addGPUTask("ChunkUpload", 1) { // change back to GPU thread
                chunkRenderer.set(chunkId, MeshEntry(mesh, bounds, data))
            }
        }
    }

    fun AABBf.translate(dx: Float, dy: Float, dz: Float) {
        minX += dx
        minY += dy
        minZ += dz
        maxX += dx
        maxY += dy
        maxZ += dz
    }

    fun loadChunks(center: Vector3i) {
        for (idx in loadingPattern) {
            val vec = Vector3i(idx).add(center)
            if (loadedChunks.add(vec)) {
                worker += { generateChunk(vec) }
                break
            }
        }
    }

    fun unloadChunks(center: Vector3i) {
        for (idx in unloadingPattern) {
            val vec = Vector3i(idx).add(center)
            if (loadedChunks.remove(vec)) {
                chunkRenderer.remove(vec)
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

    override fun onUpdate(): Int {
        // load next mesh
        if (worker.remaining <= worker.numThreads) {
            val chunkId = getPlayerChunkId()
            loadChunks(chunkId)
            unloadChunks(chunkId)
        }
        return 1
    }
}