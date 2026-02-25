package me.anno.minecraft.ui.components

import me.anno.gpu.Clipping
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.maths.Maths.clamp
import me.anno.minecraft.rendering.v2.player
import me.anno.minecraft.ui.XpLevels
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.withAlpha
import kotlin.math.max
import kotlin.math.min

class XpBarPanel(style: Style) : Panel(style) {

    private val font = monospaceFont

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = font.sizeInt
    }

    init {
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.MAX
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val xp = player.experience
        val level = XpLevels.getLevel(xp)
        val progress = 0.9f * calculateProgress(xp, level) + 0.05f
        drawBackground(x0, y0, x1, y1)
        val wi = (width * progress).toInt()
        drawRect(x, y, wi, height, UIColors.greenYellow)
        // left side black, right side yellow
        val levelStr = level.toString()
        Clipping.clip2(x0, y0, min(x0 + wi, x1), y1) {
            drawText(
                x + width / 2, y + height / 2, 0,
                font, levelStr,
                background.color.withAlpha(255), UIColors.greenYellow,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
        Clipping.clip2(x0 + wi, y0, x1, y1) {
            drawText(
                x + width / 2, y + height / 2, 0,
                font, levelStr,
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