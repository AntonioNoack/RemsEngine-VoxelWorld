package me.anno.remcraft.block

import me.anno.maths.Maths.clamp
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgb
import javax.imageio.ImageIO

object BlockColor {
    private val colors = run {
        val image = ImageIO.read(javaClass.getResource("/textures/blocks/Blocks.png"))
        IntArray(16 * 32) { texId ->
            var r = 0
            var g = 0
            var b = 0
            val x = texId and 15
            val y = texId shr 4
            val sz = 16
            val x0 = x * sz
            val y0 = y * sz
            val x1 = x0 + sz
            val y1 = y0 + sz
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val color = image.getRGB(x, y)
                    r += color.r()
                    g += color.g()
                    b += color.b()
                }
            }
            rgb(r shr 8, g shr 8, b shr 8)
        }
    }

    fun getBlockColor(texId: Int): Int = colors[clamp(texId, 0, colors.lastIndex)]
}