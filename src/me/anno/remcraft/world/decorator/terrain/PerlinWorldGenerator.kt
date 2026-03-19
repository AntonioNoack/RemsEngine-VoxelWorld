package me.anno.remcraft.world.decorator.terrain

import me.anno.maths.noise.PerlinNoise
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.decorator.Decorator
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
) : Decorator() {

    val minY = -1024
    val heightNoise = PerlinNoise(seed, log2(maxHeight - minHeight).roundToInt(), 0.5f, minHeight, maxHeight)

    fun getHeightAt(x: Int, z: Int) = heightNoise[x * scale, z * scale].toInt()

    override fun decorate(chunk: Chunk) {
        if (chunk.y0 !in minY until heightNoise.max.toInt()) return

        val x0 = chunk.x0
        val z0 = chunk.z0
        for (x in 0 until sizeX) {
            for (z in 0 until sizeZ) {
                val height0 = getHeightAt(x + x0, z + z0)
                for (y in max(0, height0 - waterLevel) until min(waterLevel - chunk.y0, sizeY)) {
                    chunk.setBlockQuickly(x, y, z, waterBlock)
                }
                val heightY = height0 - chunk.y0
                val offset = -heightY + blocks.size
                for (y in 0 until min(heightY, sizeY)) {
                    chunk.setBlockQuickly(x, y, z, blocks[max(y + offset, 0)])
                }
            }
        }
    }

}