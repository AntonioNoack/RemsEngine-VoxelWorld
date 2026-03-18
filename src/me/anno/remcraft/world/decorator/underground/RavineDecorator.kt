package me.anno.remcraft.world.decorator.underground

import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.decorator.NNNDecorator
import me.anno.remcraft.world.decorator.underground.CaveUtils.carveEllipsoid
import org.joml.Vector3i
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class RavineDecorator(density: Float, seed: Long) :
    NNNDecorator(density, Vector3i(240, 120, 240), seed) {

    override fun decorate(chunk: Chunk, lx: Int, ly: Int, lz: Int) {

        var x = lx.toFloat()
        var y = ly.toFloat()
        var z = lz.toFloat()

        val sx = lx + chunk.x0
        val sy = ly + chunk.y0
        val sz = lz + chunk.x0
        var rnd = 20

        var yaw = random[sx, sy, sz, rnd++] * PI * 2.0
        var pitch = (random[sx, sy, sz, rnd++] - 0.5) * 0.2
        val length = (random[sx, sy, sz, rnd++] * 120 + 80).toInt()
        val baseWidth = random[sx, sy, sz, rnd++] * 3.0 + 3.0
        val verticalScale = 3.0 + random[sx, sy, sz, rnd++] * 3.0 // key for ravine look

        for (i in 0 until length) {

            val dx = cos(pitch) * cos(yaw)
            val dy = sin(pitch)
            val dz = cos(pitch) * sin(yaw)

            x += dx.toFloat()
            y += dy.toFloat()
            z += dz.toFloat()

            // smoother direction change than caves
            yaw += (random[sx, sy, sz, rnd++] - 0.5) * 0.1
            pitch *= 0.8
            pitch += (random[sx, sy, sz, rnd++] - 0.5) * 0.05

            val width = baseWidth * (1.0 + sin(i * PI / length))
            carveEllipsoid(
                chunk,
                x, y, z,
                width.toFloat(),
                (width * verticalScale).toFloat(), // tall!
                width.toFloat()
            )
        }
    }
}