package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.item.RightClickBlock
import me.anno.minecraft.item.RightClickItem
import me.anno.minecraft.world.Dimension

open class CreativeControls(
    player: PlayerEntity, dimension: Dimension, renderer: RenderView
) : MinecraftControls(player, dimension, renderer) {

    override val canFly: Boolean
        get() = true

    override fun onUpdate() {
        super.onUpdate()
        applyPlayerMovement()
        updatePlayerCamera()
    }

    val dropItems = true

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (inventoryUI.isVisible) {
            inventoryUI.isVisible = false
            return
        }

        // find, which block was clicked
        // expensive way, using raycasting:
        val query = clickCast()
        when (button) {
            Key.BUTTON_LEFT -> {
                // remove block
                query ?: return
                val coords = getCoords(query, +clickDistanceDelta)
                val dropped = getBlock(coords) ?: BlockRegistry.Air
                if (dropped != BlockRegistry.Air) {
                    val droppedMetadata = getBlockMetadata(coords)
                    setBlock(coords, BlockRegistry.Air, null)
                    if (dropItems) dropped.dropAsItem(coords.x, coords.y, coords.z, droppedMetadata)
                }
            }
            Key.BUTTON_RIGHT -> {
                // add block
                val item = inHandItem
                if (query != null) {
                    val activeCoords = getCoords(query, +clickDistanceDelta)
                    val activeBlock = getBlock(activeCoords)
                    if (activeBlock is RightClickBlock && !Input.isShiftDown) {
                        activeBlock.onRightClick(this, activeCoords)
                    } else {
                        val placeCoords = getCoords(query, -clickDistanceDelta)
                        if (item is BlockType && item != BlockRegistry.Air) {
                            setBlock(placeCoords, item, inHandMetadata)
                        } else if (item is RightClickItem && !Input.isShiftDown) {
                            item.onRightClick(this, placeCoords)
                        }
                    }
                } else if (item is RightClickItem && !Input.isShiftDown) {
                    item.onRightClick(this, null)
                }
            }
            Key.BUTTON_MIDDLE -> {
                // get block
                query ?: return
                val coords = getCoords(query, +clickDistanceDelta)
                val slot = inventory.slots[inHandSlot]
                val found = getBlock(coords) ?: BlockRegistry.Air
                if (found != BlockRegistry.Air) {
                    slot.set(found, 1, getBlockMetadata(coords))
                }
            }
            else -> {}
        }
    }
}