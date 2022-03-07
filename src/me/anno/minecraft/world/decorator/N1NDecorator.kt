package me.anno.minecraft.world.decorator

import me.anno.maths.noise.FullNoise
import me.anno.minecraft.world.Chunk

abstract class N1NDecorator(val density: Float, seed: Long = 5123L) : Decorator() {

    private val random = FullNoise(seed)

    abstract fun decorate(chunk: Chunk, lx: Int, lz: Int)

    override fun decorate(chunk: Chunk) {

        val dim = chunk.dim
        val instancesPerChunk = density * dim.sizeX * dim.sizeZ

        // if 2d, let seed only depend on xz, else y as well
        for (cz in -1..1) {
            for (cx in -1..1) {
                // round based on chunk random
                val ix = cx + chunk.chunkX
                val iz = cz + chunk.chunkZ
                val rounding = random.getValue(ix, iz)
                val ipc = (instancesPerChunk + rounding).toInt()
                for (i in 0 until ipc) {
                    val bx = (random.getValue(ix, iz, i) * dim.sizeX).toInt() + cx * dim.sizeX
                    val bz = (random.getValue(ix, iz, -1 - i) * dim.sizeZ).toInt() + cz * dim.sizeZ
                    decorate(chunk, bx, bz)
                }
            }
        }

    }

}