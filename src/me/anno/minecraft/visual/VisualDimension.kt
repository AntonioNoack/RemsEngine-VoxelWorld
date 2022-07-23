package me.anno.minecraft.visual

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugAction
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.patterns.SpiralPattern
import me.anno.minecraft.block.BlockType.Companion.Dirt
import me.anno.minecraft.block.BlockType.Companion.Grass
import me.anno.minecraft.block.BlockType.Companion.Sand
import me.anno.minecraft.block.BlockType.Companion.Sandstone
import me.anno.minecraft.block.BlockType.Companion.Stone
import me.anno.minecraft.block.BlockType.Companion.Water
import me.anno.minecraft.entity.Player
import me.anno.minecraft.multiplayer.MCProtocol
import me.anno.minecraft.world.Dimension
import me.anno.minecraft.world.decorator.CactiDecorator
import me.anno.minecraft.world.decorator.PyramidDecorator
import me.anno.minecraft.world.decorator.TreeDecorator
import me.anno.minecraft.world.generator.Generator
import me.anno.minecraft.world.generator.Perlin3dWorldGenerator
import me.anno.minecraft.world.generator.PerlinWorldGenerator
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.Vector3i
import kotlin.math.floor

// todo why is the generator generating seemingly random chunks in the wild?
@Suppress("MemberVisibilityCanBePrivate")
class VisualDimension : Component() {

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

    var viewDistance = 2
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
    var chunksPerFrame = 5

    @NotSerializedProperty
    private val visualChunks = HashMap<Vector3i, VisualChunk>()

    @NotSerializedProperty
    private var generationOrder = createViewOrder(dimension.generator, viewDistance)

    @DebugAction
    fun reset() {
        visualChunks.removeIf { (_, visualChunk) ->
            if (visualChunk.entity != null) {
                visualChunk.getMesh().destroy()
                entity!!.remove(visualChunk.entity!!)
                true
            } else false
        }
    }

    private val anchor = Vector3i()
    private val tmp = Vector3i()

    private fun updateAnchor() {
        val position = RenderView.currentInstance?.position ?: RenderState.cameraPosition
        val px = floor(position.x / dimension.sizeX).toInt()
        val py = if (dimension.generator is PerlinWorldGenerator) 0 else floor(position.y / dimension.sizeY).toInt()
        val pz = floor(position.z / dimension.sizeZ).toInt()
        anchor.set(px, py, pz)
    }

    private fun getCoordsAtDelta(dx: Int, dy: Int, dz: Int): Vector3i {
        return tmp.set(anchor).add(dx, dy, dz)
    }

    fun defineChunk(delta: Vector3i): Pair<Vector3i, VisualChunk>? {
        val v = getCoordsAtDelta(delta.x, delta.y, delta.z)
        var visuals = visualChunks[v]
        if (visuals != null) return null
        visuals = VisualChunk()
        val v2 = Vector3i(v)
        visualChunks[v2] = visuals
        return Pair(v2, visuals)
    }

    private fun generateChunk(v: Vector3i, visuals: VisualChunk): Boolean {
        val chunk = dimension.getChunk(v.x, v.y, v.z, -1) ?: return false
        visuals.chunk = chunk
        val entity2 = Entity("Chunk ${v.x},${v.y},${v.z}")
        val transform = entity2.transform
        val pos = JomlPools.vec3d.borrow()
        transform.localPosition = pos.set(
            v.x * dimension.sizeX.toDouble(),
            v.y * dimension.sizeY.toDouble(),
            v.z * dimension.sizeZ.toDouble()
        )
        transform.invalidateLocal()
        transform.teleportUpdate()
        // generate mesh async as well
        visuals.ensureBuffer()
        addEvent {
            if (chunk.dim === dimension) {
                entity2.add(visuals)
                val entity1 = this.entity!!
                entity1.add(entity2)
                entity1.invalidateAABBsCompletely()
            } else {
                val chunk2 = visualChunks.remove(v)
                if (chunk2 !== visuals) visualChunks[v] = visuals
            }
        }
        return true
    }

    private fun updateChunk(dx: Int, dy: Int, dz: Int) {
        val v = getCoordsAtDelta(dx, dy, dz)
        val visuals = visualChunks[v]
        if (visuals != null) {
            visuals.wasSeen = true
        }
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
            updateChunk(delta.x, delta.y, delta.z)
            if (chunkGenQueue.size < chunksPerFrame) {
                val pair = defineChunk(delta)
                if (pair != null) {
                    chunkGenQueue += { generateChunk(pair.first, pair.second) }
                    chunkGenQueue.start()
                }
            }
        }

        visualChunks.values.removeIf { vc ->
            if (!vc.wasSeen && vc.entity != null) {
                entity!!.remove(vc.entity!!)
                // free memory
                val chunk = vc.chunk
                if (chunk != null) {
                    dimension.unload(chunk)
                    vc.getMesh().destroy()
                }
                true
            } else false
        }

        return 1 // 1 = call this function every frame
    }

    override fun onDestroy() {
        super.onDestroy()
        MCProtocol.stop(player)
    }

    override fun clone(): VisualDimension {
        val clone = VisualDimension()
        copy(clone)
        return clone
    }

    override val className: String = "VisualDimension"

    companion object {

        val decorators = listOf(
            TreeDecorator(0.03f, 5123L),
            PyramidDecorator(Sandstone, 10, Sand, 0.00001f, 49651L),
            PyramidDecorator(Sandstone, 20, Sand, 0.00001f / 3f, 19651L),
            PyramidDecorator(Sandstone, 27, Sand, 0.00001f / 9f, 29651L),
            CactiDecorator(0.001f, 97845L)
        )

        val perlin2dDim = Dimension(
            PerlinWorldGenerator(
                listOf(Stone, Dirt, Dirt, Dirt, Grass),
                Water, 30, 0.02f, 0f, 100f, 1234L
            ),
            decorators
        )

        val perlin3dDim = Dimension(
            Perlin3dWorldGenerator(listOf(Stone, Dirt, Dirt, Dirt, Grass), 1234L),
            decorators
        )

        val sandDim = Dimension(
            PerlinWorldGenerator(
                listOf(Stone, Sand, Sand),
                Stone, 5, 0.015f, 0f, 30f, 5123L
            ),
            decorators
        )

        val chunkGenQueue = ProcessingGroup("ChunkGen", 12)
        fun createViewOrder(generator: Generator, viewDistance: Int): MutableList<Vector3i> {
            return if (generator is PerlinWorldGenerator) {
                SpiralPattern.roundSpiral2d(viewDistance, 0, false)
                    .map { xz -> Array(4) { y -> Vector3i(xz.x, y, xz.z) }.toList() }
                    .flatten()
            } else {
                SpiralPattern.spiral3d(viewDistance, false)
            }.toMutableList()
        }
    }

}