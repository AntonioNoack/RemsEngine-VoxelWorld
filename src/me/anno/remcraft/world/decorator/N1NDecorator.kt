package me.anno.remcraft.world.decorator

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.noise.FullNoise
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeZ
import org.joml.Vector3i

abstract class N1NDecorator(val density: Float, val maxExtends: Vector3i, seed: Long = 5123L) : Decorator() {

    val random = FullNoise(seed)

    abstract fun decorate(chunk: Chunk, lx: Int, lz: Int)

    override fun decorate(chunk: Chunk) {

        val instancesPerChunk = density * sizeX * sizeZ
        val dcx = ceilDiv(maxExtends.x, sizeX)
        val dcz = ceilDiv(maxExtends.z, sizeZ)

        // if 2d, let seed only depend on xz, else y as well
        for (cz in -dcz..dcz) {
            for (cx in -dcx..dcx) {
                // round based on chunk random
                val ix = cx + chunk.xi
                val iz = cz + chunk.zi
                val rounding = random[ix, iz]
                val ipc = (instancesPerChunk + rounding).toInt()
                for (i in 0 until ipc) {
                    val lx = (random[ix, iz, i] * sizeX).toInt() + cx * sizeX
                    val lz = (random[ix, iz, -1 - i] * sizeZ).toInt() + cz * sizeZ
                    decorate(chunk, lx, lz)
                }
            }
        }

    }

}