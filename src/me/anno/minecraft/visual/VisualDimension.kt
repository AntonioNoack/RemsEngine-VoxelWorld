package me.anno.minecraft.visual

import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.maths.patterns.SpiralPattern
import me.anno.minecraft.entity.Player
import me.anno.minecraft.multiplayer.MCProtocol
import me.anno.minecraft.world.SampleDimensions.perlin2dDim
import me.anno.minecraft.world.SampleDimensions.perlin3dDim
import me.anno.minecraft.world.SampleDimensions.sandDim
import me.anno.minecraft.world.generator.Generator
import me.anno.minecraft.world.generator.PerlinWorldGenerator
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.Vector3i
import kotlin.math.floor

@Suppress("MemberVisibilityCanBePrivate")
class VisualDimension : MeshSpawner() {

    var usePrettyLoadingPattern = false
        set(value) {
            if (field != value) {
                field = value
                generationOrder = createViewOrder(dimension.generator, viewDistance)
            }
        }

    var terrainType = TerrainType.FOREST3D
        set(value) {
            if (field != value) {
                field = value
                reset()
                generationOrder = createViewOrder(dimension.generator, viewDistance)
            }
        }

    var viewDistance = 10
        set(value) {
            if (field != value) {
                field = value
                generationOrder = createViewOrder(dimension.generator, viewDistance)
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
    private var generationOrder = createViewOrder(dimension.generator, viewDistance)

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
        val px = floor(position.x / dimension.sizeX).toInt()
        val py = if (dimension.generator is PerlinWorldGenerator) 0 else floor(position.y / dimension.sizeY).toInt()
        val pz = floor(position.z / dimension.sizeZ).toInt()
        anchor.set(px, py, pz)
    }

    private fun getCoordsAtDelta(dx: Int, dy: Int, dz: Int): Vector3i {
        return tmp.set(anchor).add(dx, dy, dz)
    }

    val player = Player()

    override fun onUpdate(): Int {

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
                    val chunk = dimension.getChunk(safeV.x, safeV.y, safeV.z, -1)
                    visualChunk.chunk = chunk
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

        return 1 // 1 = call this function every frame
    }

    override fun forEachMesh(run: (IMesh, Material?, Transform) -> Unit) {
        var i = 0
        for ((v, visualChunk) in visualChunks) {
            if (visualChunk.hasMesh) {
                val transform = getTransform(i++)
                transform.setLocalPosition(
                    v.x * dimension.sizeX.toDouble(),
                    v.y * dimension.sizeY.toDouble(),
                    v.z * dimension.sizeZ.toDouble()
                )
                run(visualChunk.mesh, Material.defaultMaterial, transform)
            }
        }
    }

    override fun destroy() {
        super.destroy()
        MCProtocol.stop(player)
    }

    override fun clone(): VisualDimension {
        val clone = VisualDimension()
        copyInto(clone)
        return clone
    }

    override val className: String = "VisualDimension"

    companion object {

        val chunkGenQueue = ProcessingGroup("ChunkGen", 1f)
        fun createViewOrder(generator: Generator, viewDistance: Int): MutableList<Vector3i> {
            return if (generator is PerlinWorldGenerator) {
                SpiralPattern.roundSpiral2d(viewDistance, 0, false)
                    .flatMap { xz -> createArrayList(4) { y -> Vector3i(xz.x, y, xz.z) } }
            } else {
                SpiralPattern.spiral3d(viewDistance, false)
            }.toMutableList()
        }
    }

}