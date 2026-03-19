package me.anno.remcraft.world.decorator

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.noise.FullNoise
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.Index.totalSize
import org.joml.Vector3i

/**
 * @param maxExtends maximum distance from center block in that direction; a cactus of height 3 would have (0,3,0)
 * */
abstract class NNNDecorator(
    val density: Float,
    val maxExtends: Vector3i,
    seed: Long = 5123L
) : Decorator() {

    val random = FullNoise(seed)

    abstract fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int)

    override fun decorate(chunk: Chunk) {

        val instancesPerChunk = density * totalSize
        val dcx = ceilDiv(maxExtends.x, sizeX)
        val dcy = ceilDiv(maxExtends.y, sizeY)
        val dcz = ceilDiv(maxExtends.z, sizeZ)

        // if 2d, let seed only depend on xz, else y as well
        for (cy in -dcy..dcy) {
            for (cz in -dcz..dcz) {
                for (cx in -dcx..dcx) {
                    // round based on chunk random
                    val ix = cx + chunk.xi
                    val iy = cy + chunk.yi
                    val iz = cz + chunk.zi
                    val rounding = random[ix, iy, iz]
                    val ipc = (instancesPerChunk + rounding).toInt()
                    for (i in 0 until ipc) {
                        val bx = (random[ix, iy, iz, i] * sizeX).toInt() + cx * sizeX
                        val by = (random[ix, iy, iz, i + 2 * ipc] * sizeY).toInt() + cy * sizeY
                        val bz = (random[ix, iy, iz, -1 - i] * sizeZ).toInt() + cz * sizeZ
                        decorate(chunk, bx, by, bz)
                    }
                }
            }
        }
    }
}