package me.anno.remcraft.world.decorator.terrain

import me.anno.image.ImageWriter
import me.anno.remcraft.block.BlockRegistry.Air
import me.anno.remcraft.block.BlockRegistry.Stone
import me.anno.remcraft.block.BlockRegistry.Water
import me.anno.remcraft.world.Dimension

fun main() {
    val gen = PerlinWorldGenerator(listOf(Stone), Water, 30, 0.02f, 0f, 255f, 1234L)
    val dim = Dimension(listOf(gen))
    ImageWriter.writeImageInt(100, 100, false, "255", 16) { x, z, _ ->
        var maxY = 255
        for (y in maxY downTo 0) {
            if (dim.getBlockAt(x, y, z, Int.MAX_VALUE)!! != Air) {
                break
            }
            maxY--
        }
        maxY * 0x10101
    }
}