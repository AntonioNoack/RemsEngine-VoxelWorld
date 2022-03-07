package me.anno.minecraft.world.decorator

import me.anno.maths.noise.FullNoise
import me.anno.minecraft.world.Chunk

abstract class NNNDecorator(val density: Float, seed: Long = 5123L) : Decorator() {

    private val random = FullNoise(seed)

    abstract fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int)

    override fun decorate(chunk: Chunk) {

        val dim = chunk.dim
        val instancesPerChunk = density * dim.totalSize

        // if 2d, let seed only depend on xz, else y as well
        for (cy in -1..1) {
            for (cz in -1..1) {
                for (cx in -1..1) {
                    // round based on chunk random
                    val ix = cx + chunk.chunkX
                    val iy = cy + chunk.chunkY
                    val iz = cz + chunk.chunkZ
                    val rounding = random.getValue(ix, iy, iz)
                    val ipc = (instancesPerChunk + rounding).toInt()
                    for (i in 0 until ipc) {
                        val bx = (random.getValue(ix, iy, iz, i) * dim.sizeX).toInt() + cx * dim.sizeX
                        val by = (random.getValue(ix, iy, iz, i + 2 * ipc) * dim.sizeY).toInt() + cy * dim.sizeY
                        val bz = (random.getValue(ix, iy, iz, -1 - i) * dim.sizeZ).toInt() + cz * dim.sizeZ
                        decorate(chunk, bx, by, bz)
                    }
                }
            }
        }

    }

}