package me.anno.minecraft.visual

import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.world.Chunk
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager

class VisualChunk() : ProceduralMesh() {

    // todo sort triangles by side normal, back 2 front,
    // this will allow us to sort all water faces just by sorting the chunks

    constructor(chunk: Chunk?) : this() {
        this.chunk = chunk
    }

    var chunk: Chunk? = null
        set(value) {
            if (field !== value) {
                field = value
                invalidateMesh()
            }
        }

    var wasSeen = true

    override fun generateMesh() {
        // todo handle transparent blocks slightly differently
        val chunk = chunk ?: return
        val t0 = System.nanoTime()
        val dim = chunk.dim
        ChunkVoxelModel(chunk)
            .createMesh(palette, { dx, dy, dz ->
                val x = chunk.x0 + dx
                val y = chunk.y0 + dy
                val z = chunk.z0 + dz
                dim.getElementAt(x, y, z, true) != Air
            }, mesh2)
        val t1 = System.nanoTime()
        if (printTimes) LOGGER.info("mesh ${((t1 - t0) * 1e-6).f3()}ms/c")
    }

    override fun clone(): VisualChunk {
        val clone = VisualChunk()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as VisualChunk
        clone.chunk = chunk
        clone.wasSeen = wasSeen
    }

    override val className: String = "VisualChunk"

    companion object {

        private val LOGGER = LogManager.getLogger(VisualChunk::class)
        var printTimes = false

        val palette = IntArray(BlockType.library.maxOf { it.id } + 1)

        init {
            for (block in BlockType.library) {
                palette[block.id.toInt()] = block.color
            }
        }

    }

}