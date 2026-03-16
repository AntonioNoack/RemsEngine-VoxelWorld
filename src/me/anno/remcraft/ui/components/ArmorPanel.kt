package me.anno.remcraft.ui.components

import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment

class ArmorPanel(style: Style) : Panel(style) {

    private val font = monospaceFont

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = font.sizeInt
    }

    init {
        weight = 1f
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.MAX
        makeBackgroundTransparent()
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

    }

}