package me.anno.minecraft.world.generator

import me.anno.image.ImageWriter
import me.anno.maths.noise.PerlinNoise
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.BlockType.Companion.Air
import me.anno.minecraft.block.BlockType.Companion.Stone
import me.anno.minecraft.block.BlockType.Companion.Water
import me.anno.minecraft.world.Chunk
import me.anno.minecraft.world.Dimension
import kotlin.math.max
import kotlin.math.min

class PerlinWorldGenerator(val blocks: List<BlockType>, seed: Long) : Generator() {

    val heightNoise = PerlinNoise(seed, 8, 0.5f, 1f, 100f)

    val scale = 1f / 50f

    var waterLevel = 30

    fun getHeightAt(x: Int, z: Int) = heightNoise[x * scale, z * scale].toInt()

    override fun generate(chunk: Chunk) {
        val dim = chunk.dim
        val x0 = chunk.x0
        val z0 = chunk.z0
        if (chunk.y0 in 0 until heightNoise.max.toInt()) {
            for (x in 0 until dim.sizeX) {
                for (z in 0 until dim.sizeZ) {
                    val height0 = getHeightAt(x + x0, z + z0)
                    for (y in max(0, height0 - waterLevel) until min(waterLevel - chunk.y0, dim.sizeY)) {
                        chunk.setBlockQuickly(x, y, z, Water)
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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val gen = PerlinWorldGenerator(listOf(Stone), 1234L)
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