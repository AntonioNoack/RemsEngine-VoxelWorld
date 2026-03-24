package me.anno.remcraft.block

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import javax.imageio.ImageIO

object BlockColor {

    const val NUM_TEX_X = 32
    const val NUM_TEX_Y = 32

    private val colors = run {
        val image = ImageIO.read(javaClass.getResource("/textures/blocks/Blocks.png"))
        IntArray(NUM_TEX_X * NUM_TEX_Y) { texId ->
            var r = 0
            var g = 0
            var b = 0
            var a = 0
            val x = texId % NUM_TEX_X
            val y = texId / NUM_TEX_X
            val tileSize = 16
            val x0 = x * tileSize
            val y0 = y * tileSize
            val x1 = x0 + tileSize
            val y1 = y0 + tileSize
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val color = image.getRGB(x, y)
                    r += color.r()
                    g += color.g()
                    b += color.b()
                    a += color.a()
                }
            }
            val count = sq(tileSize)
            rgba(r / count, g / count, b / count, a / count)
        }
    }

    fun getBlockColor(texId: Int): Int = colors[clamp(texId, 0, colors.lastIndex)]
}