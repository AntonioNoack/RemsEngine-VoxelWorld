package me.anno.minecraft.world.decorator

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.noise.FullNoise
import me.anno.minecraft.world.Chunk
import org.joml.Vector3i

/**
 * @param maxExtends maximum distance from center block in that direction; a cactus of height 3 would have (0,3,0)
 * */
abstract class NNNDecorator(
    val density: Float,
    val maxExtends: Vector3i,
    seed: Long = 5123L
) : Decorator() {

    private val random = FullNoise(seed)

    abstract fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int)

    override fun decorate(chunk: Chunk) {

        val dim = chunk.dimension
        val instancesPerChunk = density * dim.totalSize
        val dcx = ceilDiv(maxExtends.x, dim.sizeX)
        val dcy = ceilDiv(maxExtends.y, dim.sizeY)
        val dcz = ceilDiv(maxExtends.z, dim.sizeZ)

        // if 2d, let seed only depend on xz, else y as well
        for (cy in -dcy..dcy) {
            for (cz in -dcz..dcz) {
                for (cx in -dcx..dcx) {
                    // round based on chunk random
                    val ix = cx + chunk.chunkX
                    val iy = cy + chunk.chunkY
                    val iz = cz + chunk.chunkZ
                    val rounding = random[ix, iy, iz]
                    val ipc = (instancesPerChunk + rounding).toInt()
                    for (i in 0 until ipc) {
                        val bx = (random[ix, iy, iz, i] * dim.sizeX).toInt() + cx * dim.sizeX
                        val by = (random[ix, iy, iz, i + 2 * ipc] * dim.sizeY).toInt() + cy * dim.sizeY
                        val bz = (random[ix, iy, iz, -1 - i] * dim.sizeZ).toInt() + cz * dim.sizeZ
                        decorate(chunk, bx, by, bz)
                    }
                }
            }
        }
    }
}