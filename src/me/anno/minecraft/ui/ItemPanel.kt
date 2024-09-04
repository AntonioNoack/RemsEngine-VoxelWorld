package me.anno.minecraft.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.GFX
import me.anno.input.Key
import me.anno.io.files.InvalidRef
import me.anno.mesh.Shapes.flatCube
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.v2.CreativeControls.Companion.inHandSlot
import me.anno.minecraft.v2.CreativeControls.Companion.inventory
import me.anno.ui.base.buttons.TextButton.Companion.drawButtonBorder
import me.anno.ui.utils.ThumbnailPanel
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import me.anno.utils.structures.maps.LazyMap
import kotlin.math.min

class ItemPanel(val slot: ItemSlot, val index: Int) : ThumbnailPanel(InvalidRef, style) {

    companion object {
        val previewBlocks = LazyMap { type: BlockType ->
            if (type == BlockType.Air) null
            else {
                val color = -1 // TestWorld.colors[id]!!.withAlpha(255)
                val mesh = flatCube.front.clone() as Mesh
                val materialI = Material.diffuse(color)
                MeshComponent(mesh, materialI)
            }
        }
    }

    val leftColor = style.getColor("borderColorLeft", black or 0x999999)
    val rightColor = style.getColor("borderColorRight", black or 0x111111)
    val topColor = style.getColor("borderColorTop", black or 0x999999)
    val bottomColor = style.getColor("borderColorBottom", black or 0x111111)

    val borderSize = style.getPadding("borderSize", 2)
    val bg0 = backgroundColor
    val bg1 = mixARGB(backgroundColor, white, 0.5f)
    var isPressed = false

    override fun onUpdate() {
        super.onUpdate()
        source = previewBlocks[slot.type]?.ref ?: InvalidRef
        backgroundColor = if (inHandSlot == index) bg1 else bg0
    }

    override fun calculateSize(w: Int, h: Int) {
        val size = min(64, GFX.someWindow.width / 11)
        minW = size
        minH = size
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        super.onKeyDown(x, y, key)
        isPressed = true
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        super.onKeyUp(x, y, key)
        isPressed = false
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (index in inventory.slots.indices) {
            inHandSlot = index
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        drawButtonBorder(
            leftColor, topColor, rightColor, bottomColor,
            true, borderSize, isPressed
        )
    }
}