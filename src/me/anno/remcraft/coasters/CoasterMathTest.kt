package me.anno.remcraft.coasters

import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {

    val a = CoasterRailBase(Vector3d(-10.0, +1.0, -16.0), Quaternionf().rotationY(90f.toRadians()))
    val b = CoasterRailBase(Vector3d(0.0, -1.0, -10.0), Quaternionf())
    val c = CoasterRailBase(Vector3d(0.0, +1.0, +10.0), Quaternionf())

    for (strip in listOf(CoasterRail(a, b), CoasterRail(b, c))) {
        val n = 10000
        val pos = Vector3d()
        repeat(n) {
            val t = it / (n - 1.0)
            strip.getPointAt(t, pos)
            val closestT = strip.findClosestT(pos)
            assertEquals(t, closestT, 1e-3)
        }
    }

}