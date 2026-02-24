package me.anno.minecraft.ui

import me.anno.maths.Maths.clamp
import org.recast4j.IntArrayList
import kotlin.math.pow

object XpLevels {

    // 10/1.1 -> 176 levels
    private const val BASE = 10.0
    private const val POWER = 1.1

    private val levels = IntArrayList()

    init {
        var xp = 0.0
        val limit = Int.MAX_VALUE.toDouble()
        while (xp < limit) {
            levels.add(xp.toInt())
            xp += BASE * POWER.pow(levels.size)
        }
    }

    fun getLevel(xp: Int): Int {
        val index = levels.values.binarySearch(xp, 0, levels.size)
        return if (index < 0) -index - 2 else index
    }

    fun getXp(level: Int): Int {
        return levels[clamp(level, 0, levels.size - 1)]
    }

    val MAX_LEVEL = levels.size - 1
}
