package me.anno.minecraft.ui

import me.anno.gpu.Clipping
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.maths.Maths.clamp
import me.anno.minecraft.rendering.v2.player
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.withAlpha
import kotlin.math.max
import kotlin.math.min

class XpBarPanel(style: Style) : Panel(style) {

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = monospaceFont.sizeInt
    }

    init {
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.MAX
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val xp = player.experience
        // todo render level number
        val level = XpLevels.getLevel(xp)
        val progress = 0.9f * calculateProgress(xp, level) + 0.05f
        drawBackground(x0, y0, x1, y1)
        val wi = (width * progress).toInt()
        drawRect(x, y, wi, height, UIColors.greenYellow)
        // left side black, right side yellow
        Clipping.clip2(x0, y0, min(x0 + wi, x1), y1) {
            drawText(
                x + width / 2, y + height / 2, 0,
                monospaceFont, level.toString(),
                background.color.withAlpha(255), UIColors.greenYellow,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
        Clipping.clip2(x0 + wi, y0, x1, y1) {
            drawText(
                x + width / 2, y + height / 2, 0,
                monospaceFont, level.toString(),
                UIColors.greenYellow, background.color,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
    }

    private fun calculateProgress(xp: Int, level: Int): Float {
        val xp0 = XpLevels.getXp(level)
        val xp1 = XpLevels.getXp(level + 1)
        if (xp0 == xp1) return 1f
        return clamp((xp - xp0).toFloat() / max(xp1 - xp0, 1))
    }

}