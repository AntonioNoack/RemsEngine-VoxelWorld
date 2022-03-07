package me.anno.minecraft.visual

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugAction
import me.anno.engine.ui.render.RenderView
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.patterns.SpiralPattern
import me.anno.minecraft.block.BlockType.Companion.Dirt
import me.anno.minecraft.block.BlockType.Companion.Grass
import me.anno.minecraft.block.BlockType.Companion.Stone
import me.anno.minecraft.multiplayer.MCProtocol
import me.anno.minecraft.world.Dimension
import me.anno.minecraft.world.decorator.TreeDecorator
import me.anno.minecraft.world.generator.Perlin3dWorldGenerator
import me.anno.minecraft.world.generator.PerlinWorldGenerator
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.Maps.removeIf
import org.joml.Vector3i
import kotlin.math.floor

class VisualDimension : Component() {

    val decorators = listOf(
        TreeDecorator(0.03f, 5123L)
    )

    val dimension = when (1) {// can be changed to change the generator
        0 -> Dimension(PerlinWorldGenerator(listOf(Stone, Dirt, Dirt, Dirt, Grass), 1234L), decorators)
        else -> Dimension(Perlin3dWorldGenerator(listOf(Stone, Dirt, Dirt, Dirt, Grass), 1234L), decorators)
    }

    @NotSerializedProperty
    private val visualChunks = HashMap<Vector3i, VisualChunk>()

    @NotSerializedProperty
    private val generationOrder = if(dimension.generator is PerlinWorldGenerator){
        SpiralPattern.roundSpiral2d(12, 0, false)
            .map { v ->
                (if (dimension.generator is PerlinWorldGenerator)
                    Array(4) { Vector3i(v.x, it, v.z) }
                else Array(7) { Vector3i(v.x, it - 3, v.z) }).toList()
            }
            .flatten()
    } else {
        SpiralPattern.spiral3d(12, false)
    }

    @DebugAction
    fun reset() {
        visualChunks.removeIf { (_, visualChunk) ->
            if (visualChunk.entity != null) {
                visualChunk.mesh2.destroy()
                entity!!.remove(visualChunk.entity!!)
                true
            } else false
        }
    }

    // should depend on the generation rate, and be approx. the value for 3 frames
    var chunksPerFrame = 5

    private val anchor = Vector3i()
    private val tmp = Vector3i()

    private fun updateAnchor() {
        val position = RenderView.currentInstance?.position ?: RenderView.camPosition
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
        // generate mesh async as well
        visuals.ensureBuffer()
        addEvent {
            // vector needs to be cloned, because it is a temporary value
            entity2.add(visuals)
            val entity1 = this.entity!!
            entity1.add(entity2)
            transform.teleportUpdate()
            entity?.invalidateAABBsCompletely()
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

    override fun onUpdate(): Int {

        // todo prefer chunks in front of the camera

        // load & unload chunks
        for (c in visualChunks.values) {
            c.wasSeen = false
        }

        updateAnchor()
        val entity = entity
        if (entity != null) {
            MCProtocol.updatePlayers(entity)
        }

        for (delta in generationOrder) {
            updateChunk(delta.x, delta.y, delta.z)
            if (chunkGenQueue.size < chunksPerFrame) {
                val pair = defineChunk(delta)
                if (pair != null) {
                    chunkGenQueue += { generateChunk(pair.first, pair.second) }
                    chunkGenQueue.start()
                }
            }// else break
        }

        visualChunks.values.removeIf { vc ->
            if (!vc.wasSeen && vc.entity != null) {
                entity!!.remove(vc.entity!!)
                // free memory
                val chunk = vc.chunk
                if (chunk != null) {
                    dimension.unload(chunk)
                    vc.mesh2.destroy()
                }
                true
            } else false
        }

        return 1 // 1 = call this function every frame
    }

    override fun onDestroy() {
        super.onDestroy()
        MCProtocol.stop()
    }

    override fun clone(): VisualDimension {
        val clone = VisualDimension()
        copy(clone)
        return clone
    }

    override val className: String = "VisualDimension"

    companion object {
        val chunkGenQueue = ProcessingGroup("ChunkGen", 12)
    }

}