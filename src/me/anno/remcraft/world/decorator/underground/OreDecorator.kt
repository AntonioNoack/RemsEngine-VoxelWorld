package me.anno.remcraft.world.decorator.underground

import me.anno.maths.Maths
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.decorator.NNNDecorator
import me.anno.remcraft.world.decorator.Offset
import me.anno.utils.search.BinarySearch
import me.anno.utils.structures.arrays.FloatArrayList
import org.joml.Vector3i

class OreDecorator(density: Float = 0.1f, seed: Long = 5123L) :
    NNNDecorator(density, Vector3i(3), seed) {

    companion object {
        private val offsets = Array(27) { idx ->
            val rx = (idx % 3) - 1
            val ry = ((idx / 3) % 3) - 1
            val rz = (idx / 9) - 1
            val chance = 1f / (1 + rx * rx + ry * ry + rz * rz)
            Offset(rx, ry, rz, chance)
        }
    }

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {
        if (sumChance <= 0f) return
        if (chunk.getBlock(lx, ly, lz) != BlockRegistry.Stone) return
        val type = sampleType(chunk.x0 + lx, chunk.y0 + ly, chunk.z0 + lz)
        // calculate size/shape
        for (i in offsets.indices) {
            val offset = offsets[i]
            if (random[chunk.x0 + lx, chunk.y0 + ly, chunk.z0 + lz, i + 12] <= offset.chance &&
                chunk.getBlock(lx + offset.dx, ly + offset.dy, lz + offset.dz) == BlockRegistry.Stone
            ) {
                chunk.setBlockQuickly(lx + offset.dx, ly + offset.dy, lz + offset.dz, type)
            }
        }
    }

    private fun sampleType(gx: Int, gy: Int, gz: Int): BlockType {
        val sample = random[gx, gy, gz, 1] * sumChance
        var idx = BinarySearch.binarySearch(chances.size) { idx ->
            chances[idx].compareTo(sample)
        }
        if (idx < 0) idx = -1 - idx
        idx = Maths.clamp(idx, 0, chances.lastIndex)
        return types[idx]
    }

    private var sumChance = 0f
    private val types = ArrayList<BlockType>()
    private val chances = FloatArrayList()

    fun addOreType(type: BlockType, relativeChance: Float): OreDecorator {
        if (relativeChance > 0f) {
            types.add(type)
            sumChance += relativeChance
            chances.add(sumChance)
        }
        return this
    }
}