package me.anno.remcraft.world.decorator.underground

import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.decorator.NNNDecorator
import me.anno.remcraft.world.decorator.underground.CaveUtils.carveSphere
import org.joml.Vector3i
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CaveDecorator(density: Float, seed: Long) :
    NNNDecorator(density, Vector3i(128, 64, 128), seed) {

    override val readsPreviousStage: Boolean get() = false

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {

        var x = lx.toFloat()
        var y = ly.toFloat()
        var z = lz.toFloat()

        val sx = lx + chunk.x0
        val sy = ly + chunk.y0
        val sz = lz + chunk.z0
        var rnd = 0

        // initial direction
        var yaw = random[sx, sy, sz, rnd++] * PI * 2.0
        var pitch = (random[sx, sy, sz, rnd++] - 0.5) * 0.4

        // cave length
        val length = (random[sx, sy, sz, rnd++] * 76 + 40).toInt()
        val radius = random[sx, sy, sz, rnd++] * 2.0 + 1.0
        for (i in 0 until length) {

            val dx = cos(pitch) * cos(yaw)
            val dy = sin(pitch)
            val dz = cos(pitch) * sin(yaw)

            x += dx.toFloat()
            y += dy.toFloat()
            z += dz.toFloat()

            // slowly change direction (wiggle)
            yaw += (random[sx, sy, sz, rnd++] - 0.5) * 0.2
            pitch *= 0.7
            pitch += (random[sx, sy, sz, rnd++] - 0.5) * 0.1

            // vary radius
            val rnd = radius * (1.0 + sin(i * PI / length))

            carveSphere(chunk, x, y, z, rnd.toFloat())

            // branching (very important for MC feel)
            // todo we cannot just branch of like that, because we could leave the space...
            /*if (i == length.shr(1) && random[sx, sy, sz, -99] > 0.7) {
                decorate(chunk, x.toInt(), y.toInt(), z.toInt())
            }*/
        }
    }
}