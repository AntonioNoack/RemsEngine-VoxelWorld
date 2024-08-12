package me.anno.minecraft.world.generator

import me.anno.image.ImageWriter
import me.anno.maths.noise.PerlinNoise
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.block.BlockType.Companion.Stone
import me.anno.minecraft.block.BlockType.Companion.Water
import me.anno.minecraft.world.Chunk
import me.anno.minecraft.world.Dimension
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PerlinWorldGenerator(
    val blocks: List<BlockType>,
    val waterBlock: BlockType,
    var waterLevel: Int,
    val scale: Float,
    minHeight: Float,
    maxHeight: Float,
    seed: Long
) : Generator() {

    val heightNoise = PerlinNoise(seed, log2(maxHeight - minHeight).roundToInt(), 0.5f, minHeight, maxHeight)

    fun getHeightAt(x: Int, z: Int) = heightNoise[x * scale, z * scale].toInt()

    fun generateSurface(chunk: Chunk) {
        val dim = chunk.dim
        val x0 = chunk.x0
        val z0 = chunk.z0
        if (chunk.y0 in 0 until heightNoise.max.toInt()) {
            for (x in 0 until dim.sizeX) {
                for (z in 0 until dim.sizeZ) {
                    val height0 = getHeightAt(x + x0, z + z0)
                    for (y in max(0, height0 - waterLevel) until min(waterLevel - chunk.y0, dim.sizeY)) {
                        chunk.setBlockQuickly(x, y, z, waterBlock)
                    }
                    val heightY = height0 - chunk.y0
                    val offset = -heightY + blocks.size
                    for (y in 0 until min(heightY, dim.sizeY)) {
                        chunk.setBlockQuickly(x, y, z, blocks[max(y + offset, 0)])
                    }
                }
            }
        }
    }

    override fun generate(chunk: Chunk) {
        generateSurface(chunk)
        loadSaveData(chunk)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val gen = PerlinWorldGenerator(listOf(Stone), Water, 30, 0.02f, 0f, 255f, 1234L)
            val dim = Dimension(gen, emptyList())
            ImageWriter.writeImageInt(100, 100, false, "255", 16) { x, z, _ ->
                var maxY = 255
                for (y in maxY downTo 0) {
                    if (dim.getElementAt(x, y, z, true)!! != Air) {
                        break
                    }
                    maxY--
                }
                maxY * 0x10101
            }
        }
    }

}