package me.anno.remcraft.coasters

import me.anno.maths.Maths
import kotlin.math.abs

object GoldenSectionSearch {

    const val INV_PHI = 1.0 / Maths.PHI

    fun interface ObjectiveFunction {
        fun map(x: Double): Double
    }

    fun goldenSectionSearch(
        x0: Double,
        x1: Double,
        accuracy: Double,
        f: ObjectiveFunction,
    ): Double {

        var x0 = x0
        var x1 = x1

        var diff = x1 - x0
        var c = x1 - INV_PHI * diff
        var d = x0 + INV_PHI * diff

        var fc = f.map(c)
        var fd = f.map(d)

        while (abs(diff) > accuracy) {
            if (fc < fd) {
                x1 = d
                d = c
                fd = fc

                diff = x1 - x0
                c = x1 - INV_PHI * diff
                fc = f.map(c)
            } else {
                x0 = c
                c = d
                fc = fd

                diff = x1 - x0
                d = x0 + INV_PHI * diff
                fd = f.map(d)
            }
        }

        return (x0 + x1) * 0.5
    }
}