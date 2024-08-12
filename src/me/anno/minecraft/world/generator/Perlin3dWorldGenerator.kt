package me.anno.minecraft.world.generator

import me.anno.maths.Maths.mix
import me.anno.maths.noise.PerlinNoise
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.v2.world
import me.anno.minecraft.world.Chunk
import me.anno.utils.hpc.threadLocal
import org.joml.Vector4f
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Perlin3dWorldGenerator(blocks: List<BlockType>, seed: Long) : Generator() {

    companion object {
        val densities = threadLocal {
            FloatArray(world.sizeX * world.sizeY * world.sizeZ)
        }
    }

    val blocks = blocks.map { it.id }.toShortArray()

    val threshold = 0.5f

    val scale = 1f / 50f
    val densityY = 0.1f * scale

    val densityNoise = PerlinNoise(
        seed, max(1, (-log2(scale) - 2).roundToInt()),
        0.5f, 0f, 1f,
        Vector4f(scale)
    )

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

    fun getDensityAt(x: Int, y: Int, z: Int): Float {
        val yf = y.toFloat()
        return densityNoise[x.toFloat(), yf, z.toFloat()] - yf * densityY
    }

    fun sampleDensities(chunk: Chunk): FloatArray {
        val dim = chunk.dim
        val sx = dim.sizeX
        val sy = dim.sizeY
        val sz = dim.sizeZ
        val res = densities.get()
        val s = 4
        val m = s - 1
        // fill in base densities
        for (ly0 in 0..sy step s) {
            val ly = min(ly0, sy - 1)
            val gy = ly + chunk.y0
            for (lx0 in 0..sx step s) {
                val lx = min(lx0, sx - 1)
                val gx = lx + chunk.x0
                for (lz0 in 0..sz step s) {
                    val lz = min(lz0, sz - 1)
                    val gz = lz + chunk.z0
                    val idx = chunk.getIndex(lx, ly, lz)
                    res[idx] = getDensityAt(gx, gy, gz)
                }
            }
        }
        // fill in missing densities
        val ls = floatArrayOf(0f, 0.25f, 0.50f, 0.75f)
        for (ly in 0 until sy) {
            val y0 = ly - (ly and m)
            val y1 = min(y0 + s, sy - 1)
            val ty = ls[ly and m]
            for (lx in 0 until sx) {
                val x0 = lx - (lx and m)
                val x1 = min(x0 + s, sx - 1)
                val tx = ls[lx and m]
                for (lz in 0 until sz) {
                    val idx = chunk.getIndex(lx, ly, lz)
                    val z0 = lz - (lz and m)
                    val z1 = min(z0 + s, sz - 1)
                    val tz = ls[lz and m]
                    res[idx] = mix(
                        mix(
                            mix(
                                res[chunk.getIndex(x0, y0, z0)],
                                res[chunk.getIndex(x0, y1, z0)], ty
                            ), mix(
                                res[chunk.getIndex(x1, y0, z0)],
                                res[chunk.getIndex(x1, y1, z0)], ty
                            ), tx
                        ),
                        mix(
                            mix(
                                res[chunk.getIndex(x0, y0, z1)],
                                res[chunk.getIndex(x0, y1, z1)], ty
                            ), mix(
                                res[chunk.getIndex(x1, y0, z1)],
                                res[chunk.getIndex(x1, y1, z1)], ty
                            ), tx
                        ),
                        tz
                    )
                }
            }
        }
        return res
    }

    fun fillStone(chunk: Chunk) {
        val dim = chunk.dim
        val block = BlockType.Stone
        val sx = dim.sizeX
        val sy = dim.sizeY
        val sz = dim.sizeZ
        val densities = sampleDensities(chunk)
        for (ly in 0 until sy) {
            for (lx in 0 until sx) {
                for (lz in 0 until sz) {
                    if (
                    // 3ms -> 0.3ms
                    //  isSolid(chunk.x0 + lx, chunk.y0 + ly, chunk.z0 + lz)
                        densities[chunk.getIndex(lx, ly, lz)] > threshold
                    ) {
                        chunk.setBlockQuickly(lx, ly, lz, block)
                    }
                }
            }
        }
    }

    fun decorateSurface(chunk: Chunk) {
        val dim = chunk.dim
        val x0 = chunk.x0
        val y0 = chunk.y0
        val z0 = chunk.z0
        val blocks = blocks
        val sx = dim.sizeX
        val sy = dim.sizeY
        val sz = dim.sizeZ
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

    override fun generate(chunk: Chunk) {
        // val t0 = System.nanoTime()
        fillStone(chunk) // 81%
        // val t1 = System.nanoTime()
        decorateSurface(chunk) // 19%
        // val t2 = System.nanoTime()
        loadSaveData(chunk) // 0%
        // val t3 = System.nanoTime()
        // val total = 1f / (t3 - t0)
        // println("Generate: ${((t1 - t0) * total).f3()}, ${((t2 - t1) * total).f3()}, ${((t3 - t2) * total).f3()}, ${(1e-6f / total).f3()}ms")
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

}