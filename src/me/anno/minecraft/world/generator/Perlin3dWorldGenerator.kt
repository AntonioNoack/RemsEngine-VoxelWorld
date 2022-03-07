package me.anno.minecraft.world.generator

import me.anno.maths.noise.PerlinNoise
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.world.Chunk
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

class Perlin3dWorldGenerator(blocks: List<BlockType>, seed: Long) : Generator() {

    val blocks = blocks.map { it.id }.toShortArray()

    val threshold = 0.5f

    val scale = 1f / 50f

    val densityNoise = PerlinNoise(seed, max(1, (-log2(scale)).roundToInt()), 0.5f, 0f, 1f)

    fun getDensityAt(x: Int, y: Int, z: Int): Float {
        val scale = scale
        val xf = x * scale
        val yf = y * scale
        val zf = z * scale
        return densityNoise[xf, yf, zf] - yf * 0.1f
    }

    override fun generate(chunk: Chunk) {
        val dim = chunk.dim
        val x0 = chunk.x0
        val y0 = chunk.y0
        val z0 = chunk.z0
        val blocks = blocks
        val block = BlockType.Stone
        val sx = dim.sizeX
        val sy = dim.sizeY
        val sz = dim.sizeZ
        for (ly in 0 until sy) {
            val gy = ly + y0
            for (lx in 0 until sx) {
                val gx = lx + x0
                for (lz in 0 until sz) {
                    val gz = lz + z0
                    if (isSolid(gx, gy, gz)) {
                        chunk.setBlockQuickly(lx, ly, lz, block)
                    }
                }
            }
        }
        val bsm1 = blocks.size - 1
        val dy = chunk.getIndex(0, 1, 0) - chunk.getIndex(0, 0, 0)
        for (lx in 0 until sx) {
            val gx = lx + x0
            for (lz in 0 until sz) {
                val gz = lz + z0
                var ly = sy - 1
                var gy = ly + y0
                while (ly >= 0) {
                    if (!chunk.isAir(lx, ly, lz)) {
                        var blockIndex = bsm1 - getHeightAt(chunk, gx, gy, gz, dy, bsm1)
                        chunk.setBlockQuickly(lx, ly, lz, blocks[max(blockIndex, 0)])
                        ly--
                        gy--
                        while (ly >= 0 && !chunk.isAir(lx, ly, lz)) {
                            blockIndex--
                            chunk.setBlockQuickly(lx, ly, lz, blocks[max(blockIndex, 0)])
                            ly--
                            gy--
                        }
                    }
                    ly--
                    gy--
                }
            }
        }
    }

    fun getHeightAt(chunk: Chunk, xi: Int, yi: Int, zi: Int, dy: Int, maxHeight: Int): Int {
        val dim = chunk.dim
        var index = chunk.getIndex(xi and dim.maskX, yi and dim.maskY, zi and dim.maskZ)
        // this block is guaranteed to be solid
        for (height in 1 until maxHeight) {
            index += dy
            if (isAir(chunk, xi, yi + height, zi, index)) {
                return height - 1
            }
        }
        return maxHeight
    }

    fun isSolid(xi: Int, yi: Int, zi: Int): Boolean {
        return getDensityAt(xi, yi, zi) > threshold
    }

    fun isAir(chunk: Chunk, xi: Int, yi: Int, zi: Int, index: Int): Boolean {
        return if (yi < chunk.y1) {
            chunk.isAir(index)
        } else {
            !isSolid(xi, yi, zi)
        }
    }

}