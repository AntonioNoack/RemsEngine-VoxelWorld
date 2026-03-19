package me.anno.remcraft.rendering.v1

import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.maths.patterns.SpiralPattern
import me.anno.remcraft.entity.PlayerEntity
import me.anno.remcraft.multiplayer.MCProtocol
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.SampleDimensions.perlin2dDim
import me.anno.remcraft.world.SampleDimensions.perlin3dDim
import me.anno.remcraft.world.SampleDimensions.sandDim
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.Vector3i
import kotlin.math.floor

class VisualDimension : MeshSpawner(), OnUpdate {

    var usePrettyLoadingPattern = false
        set(value) {
            if (field != value) {
                field = value
                generationOrder = createViewOrder(viewDistance)
            }
        }

    var terrainType = TerrainType.FOREST3D
        set(value) {
            if (field != value) {
                field = value
                reset()
                generationOrder = createViewOrder(viewDistance)
            }
        }

    var viewDistance = 10
        set(value) {
            if (field != value) {
                field = value
                generationOrder = createViewOrder(viewDistance)
            }
        }

    val dimension
        get() = when (terrainType) {// can be changed to change the generator
            TerrainType.FOREST2D -> perlin2dDim
            TerrainType.FOREST3D -> perlin3dDim
            TerrainType.SAND_DESERT -> sandDim
        }

    // should depend on the generation rate, and be approx. the value for 3 frames
    var chunksPerFrame = 50

    @NotSerializedProperty
    private val visualChunks = HashMap<Vector3i, VisualChunk>()

    @NotSerializedProperty
    private var generationOrder = createViewOrder(viewDistance)

    @DebugAction
    fun reset() {
        visualChunks.removeIf { (_, visualChunk) ->
            if (visualChunk.hasMesh) {
                visualChunk.destroy()
                true
            } else false
        }
    }

    private val anchor = Vector3i()
    private val tmp = Vector3i()

    private fun updateAnchor() {
        val position = RenderView.currentInstance?.orbitCenter ?: RenderState.cameraPosition
        val px = floor(position.x / sizeX).toInt()
        val py = floor(position.y / sizeY).toInt()
        val pz = floor(position.z / sizeZ).toInt()
        anchor.set(px, py, pz)
    }

    private fun getCoordsAtDelta(dx: Int, dy: Int, dz: Int): Vector3i {
        return tmp.set(anchor).add(dx, dy, dz)
    }

    val player = PlayerEntity()

    override fun onUpdate() {

        // load & unload chunks
        for (c in visualChunks.values) {
            c.wasSeen = false
        }

        updateAnchor()

        val entity = entity
        if (entity != null) {
            MCProtocol.updatePlayers(player, entity)
        }

        // prefer chunks in front of the camera
        if (!usePrettyLoadingPattern) {
            generationOrder.sortBy {
                // [0.001, 2.001]
                val dot = RenderState.cameraDirection
                    .dot(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) + it.length() * 1.001
                it.lengthSquared() / dot
            }
        }

        for (delta in generationOrder) {
            val tmpV = getCoordsAtDelta(delta.x, delta.y, delta.z)
            var visualChunk = visualChunks[tmpV]
            visualChunk?.wasSeen = true
            if (visualChunk == null && chunkGenQueue.size < chunksPerFrame) {
                visualChunk = VisualChunk()
                val safeV = Vector3i(tmpV)
                visualChunks[safeV] = visualChunk
                chunkGenQueue += {
                    visualChunk.chunk = dimension.getChunk(safeV.x, safeV.y, safeV.z, Int.MAX_VALUE).waitFor()
                    visualChunk.generateMesh()
                }
            }
        }

        visualChunks.removeIf { (_, visualChunk) ->
            if (!visualChunk.wasSeen && visualChunk.hasMesh) {
                visualChunk.destroy()
                // free memory
                val chunk = visualChunk.chunk
                if (chunk != null) {
                    dimension.unload(chunk)
                }
                true
            } else false
        }
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        var i = 0
        for ((v, visualChunk) in visualChunks) {
            if (visualChunk.hasMesh) {
                val transform = getTransform(i++)
                transform.setLocalPosition(
                    v.x * sizeX.toDouble(),
                    v.y * sizeY.toDouble(),
                    v.z * sizeZ.toDouble()
                )
                callback(visualChunk.solidMesh, solidMaterial, transform)
                callback(visualChunk.fluidMesh, fluidMaterial, transform)
            }
        }
    }

    override fun destroy() {
        super.destroy()
        MCProtocol.stop(player)
    }

    override val className: String = "VisualDimension"

    companion object {

        val solidMaterial = Material.defaultMaterial
        val fluidMaterial = Material().apply { pipelineStage = PipelineStage.TRANSPARENT }

        val chunkGenQueue = ProcessingGroup("ChunkGen", 1f)
        fun createViewOrder(viewDistance: Int): MutableList<Vector3i> {
            return SpiralPattern.roundSpiral2d(viewDistance, 0, false)
                .flatMap { xz -> createArrayList(4) { y -> Vector3i(xz.x, y, xz.z) } }
                .toMutableList()
        }
    }

}