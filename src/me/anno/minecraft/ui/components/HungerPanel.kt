package me.anno.minecraft.ui.components

import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.maths.Maths.ceilDiv
import me.anno.minecraft.rendering.v2.player
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.withAlpha

class HungerPanel(style: Style) : Panel(style) {

    private val font = monospaceFont
    private val instance = "\uD83C\uDF56"

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val numRows = ceilDiv(player.maxHunger, 20)
        minH = font.sizeInt * numRows
    }

    init {
        weight = 1f
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.MAX
        makeBackgroundTransparent()
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // todo black/red heart,
        //  wimmering when low
        val sz = font.sizeInt
        val x = x + width - 10 * sz + sz / 2
        val y = y + height - sz / 2
        for (i in 0 until player.maxHunger / 2) {
            val xi = i % 10
            val yi = i / 10
            val color = if (player.hunger > i * 2) -1 else 0x333333.withAlpha(255)
            drawText(
                x + xi * sz, y - yi * sz, font,
                instance, color, 0,
                -1, -1,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
    }

}