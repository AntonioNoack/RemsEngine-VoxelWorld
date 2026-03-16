package me.anno.remcraft.coasters

import org.joml.Vector3d

class EquiSpline(strip: CoasterRail) {

    companion object {
        val n = 10
    }

    private val samples = DoubleArray(n)

    val positions = Array(n + 1) { idx ->
        val t = idx.toDouble() / n
        strip.getPointAt(t, Vector3d())
    }

    init {

        // set all distances
        for (i in 0 until n) {
            samples[i] = positions[i].distance(positions[i + 1])
        }

        // accumulate
        for (i in 1 until n) {
            samples[i] += samples[i - 1]
        }

        // normalize
        val invSum = 1.0 / samples.last()
        for (i in 0 until n) {
            samples[i] *= invSum
        }
    }

    operator fun get(f: Double): Double {
        for (i in 0 until n) {
            if (samples[i] >= f) {
                val s0 = if (i == 0) 0.0 else samples[i - 1]
                val s1 = samples[i]
                val t = (f - s0) / (s1 - s0)
                return (t + i) / n
            }
        }
        return f
    }
}