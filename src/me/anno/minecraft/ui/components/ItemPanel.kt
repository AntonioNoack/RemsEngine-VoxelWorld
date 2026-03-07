package me.anno.minecraft.ui.components

import me.anno.cache.FileCacheList
import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.input.Key
import me.anno.io.files.InvalidRef
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.types.DetailedBlockVisuals
import me.anno.minecraft.block.builder.BlockBuilder
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.entity.model.CuboidCreator
import me.anno.minecraft.rendering.v2.TextureMaterial
import me.anno.minecraft.rendering.v2.player
import me.anno.minecraft.ui.ItemSlot
import me.anno.minecraft.ui.controls.GameMode
import me.anno.minecraft.ui.controls.MinecraftControls.Companion.inHandSlot
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton.Companion.drawButtonBorder
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.ui.utils.ThumbnailPanel
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.res
import me.anno.utils.structures.maps.LazyMap
import kotlin.math.min

class ItemPanel(val slot: ItemSlot, val index: Int) :
    ThumbnailPanel(InvalidRef, style) {

    companion object {
        val blockMaterial = Material().apply {
            linearFiltering = false
            diffuseMap = res.getChild("textures/blocks/Blocks.png")
        }
        val previewBlocks = LazyMap { type: BlockType ->
            when (type) {
                BlockRegistry.Air -> null
                is DetailedBlockVisuals -> type.getModel().toMesh()
                else -> {
                    if (true) {
                        val mesh = CuboidCreator.createMonoCuboid(
                            16, 16, 16,
                            type.texId.and(15) * 16,
                            type.texId.shr(4) * 16,
                            getSize(256, 512)
                        )
                        mesh.materials = FileCacheList.of(blockMaterial)
                        mesh
                    } else {
                        // todo why is this variant invisible???
                        val builder = BlockBuilder()
                        builder.addCube(0, 0, 0, 16, 16, 16, type.texId)
                        val mesh = builder.build()
                        if (type.isFluid) mesh.materials = listOf(TextureMaterial.fluid.ref)
                        mesh.toMesh()
                    }
                }
            }?.ref
        }

        val leftColor = style.getColor("borderColorLeft", black or 0x999999)
        val rightColor = style.getColor("borderColorRight", black or 0x111111)
        val topColor = style.getColor("borderColorTop", black or 0x999999)
        val bottomColor = style.getColor("borderColorBottom", black or 0x111111)

        val borderSize = style.getPadding("borderSize", 2)

        private val dragged = player.inventory[PlayerEntity.EDIT_SLOT]
        private val sample = ItemPanel(dragged, PlayerEntity.EDIT_SLOT)

        fun drawDraggedItem(window: Window?, w: Int) {
            val dragCount = dragged.count
            if (dragCount <= 0 || dragged.type == BlockRegistry.Air) return
            val ws = window?.windowStack ?: return

            val mx = ws.mouseXi
            val my = ws.mouseYi
            val dx = w shr 1
            val x0 = mx - dx
            val y0 = my - dx
            sample.setPosSize(x0, y0, w, w)
            sample.onUpdate()
            sample.draw(x0, y0, x0 + w, y0 + w)
        }
    }

    var isPressed = false

    override fun onUpdate() {
        super.onUpdate()
        source = if (slot.count > 0) previewBlocks[slot.type] ?: InvalidRef else InvalidRef
    }

    val isSelected get() = inHandSlot == index || (isPressed && slot.count > 0)

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
        if (player.gameMode == GameMode.SPECTATOR) return
        if (index in 0 until 9) inHandSlot = index

        val rightClick = button == Key.BUTTON_RIGHT || long
        if (dragged.count > 0) {
            val moved =
                if (slot.count == 0 || (dragged.type == slot.type && dragged.metadata == slot.metadata)) {
                    val wantDropped = if (rightClick) 1 else dragged.count
                    min(wantDropped, dragged.type.stackingLimit - slot.count)
                } else 0
            if (moved == 0) dragged.swap(slot)
            else slot.moveFrom(dragged, moved)
        } else if (slot.count > 0) {
            startDrag(rightClick)
        }
    }

    fun startDrag(split: Boolean) {
        val count = slot.count
        val moved = if (split) (count + 1).shr(1) else count
        dragged.moveFrom(slot, moved)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val count = slot.count
        drawBackground(x0, y0, x1, y1)
        if (count > 0) drawImage()

        if (isSelected) {
            drawButtonBorder(
                rightColor, bottomColor, leftColor, topColor,
                player.gameMode != GameMode.SPECTATOR,
                borderSize, count > 0 && isPressed
            )
        } else {
            drawButtonBorder(
                leftColor, topColor, rightColor, bottomColor,
                player.gameMode != GameMode.SPECTATOR,
                borderSize, count > 0 && isPressed
            )
        }

        if (count > 1) {
            val pbb = DrawTexts.pushBetterBlending(true)
            drawText(
                x + width - 3, y + height, 0, monospaceFont, "${count}x",
                FrameTimings.textColor, background.originalColor.withAlpha(0),
                AxisAlignment.MAX, AxisAlignment.MAX
            )
            DrawTexts.popBetterBlending(pbb)
        }
    }
}