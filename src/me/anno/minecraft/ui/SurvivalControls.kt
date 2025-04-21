package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.input.Key
import me.anno.io.utils.StringMap
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.Player
import me.anno.minecraft.item.DurableItem
import me.anno.minecraft.item.DurableItem.Companion.METADATA_DURABILITY
import me.anno.minecraft.item.ItemType
import me.anno.minecraft.item.RightClickBlock
import me.anno.minecraft.item.RightClickItem
import me.anno.minecraft.rendering.v2.ChunkLoader
import me.anno.minecraft.world.Dimension
import org.joml.Vector3i

open class SurvivalControls(
    player: Player, dimension: Dimension, chunkLoader: ChunkLoader, renderer: RenderView,
    val allowsBlockPlacing: Boolean = true
) : MinecraftControls(player, dimension, chunkLoader, renderer) {

    // todo disable fly controls, and instead implement simple physics

    fun removeItemAfterPlacing() {
        val inHand = inHand
        val oldCount = inHand.count
        val newCount = oldCount - 1
        if (newCount <= 0) {
            inHand.set(BlockType.Air, 0, null)
        } else {
            inHand.count = newCount
        }
    }

    fun spawnItem(itemType: ItemType, metadata: Metadata?, position: Vector3i) {
        // todo if nearby stack with same metadata and type exists, increment count there
        // todo add item to visuals
    }

    fun removeItemDurability() {
        val inHand = inHand
        val item = inHand.type as? DurableItem ?: return
        val oldHealth = inHand.metadata?.get(METADATA_DURABILITY, item.maxHealth) ?: item.maxHealth
        val newHealth = oldHealth - 1
        if (newHealth <= 0) {
            inHand.set(BlockType.Air, 0, null)
        } else {
            val newMetadata = inHand.metadata ?: StringMap()
            newMetadata[METADATA_DURABILITY] = newHealth
            inHand.metadata = newMetadata
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = clickCast()
        when (button) {
            Key.BUTTON_LEFT -> {
                // todo check for entity-hitboxes
                if (!allowsBlockPlacing || query == null) return
                // remove block
                val coords = getCoords(query, +clickDistanceDelta)
                val dropped = getBlock(coords)
                if (dropped != BlockType.Air && !dropped.isFluid) {
                    val droppedMetadata = getBlockMetadata(coords)
                    setBlock(coords, BlockType.Air)
                    spawnItem(dropped, droppedMetadata, coords)
                    removeItemDurability()
                }
            }
            Key.BUTTON_RIGHT -> {
                val item = inHandItem
                if (query != null) {
                    val activeCoords = getCoords(query, +clickDistanceDelta)
                    val activeBlock = getBlock(activeCoords)
                    if (activeBlock is RightClickBlock) {
                        activeBlock.onRightClick(this, activeCoords)
                    } else {
                        // set block
                        val coords = getCoords(query, -clickDistanceDelta)
                        if (allowsBlockPlacing && item is BlockType && item != BlockType.Air) {
                            setBlock(coords, item)
                            removeItemAfterPlacing()
                        } else if (item is RightClickItem) {
                            item.onRightClick(this, coords)
                        }
                    }
                } else if (item is RightClickItem) {
                    item.onRightClick(this, null)
                }
            }
            else -> {}
        }
    }
}