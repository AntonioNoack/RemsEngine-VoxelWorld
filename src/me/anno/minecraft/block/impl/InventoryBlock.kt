package me.anno.minecraft.block.impl

import me.anno.config.DefaultConfig.style
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.ceilDiv
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.item.RightClickBlock
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.ui.Inventory
import me.anno.minecraft.ui.components.ItemPanel
import me.anno.minecraft.ui.controls.MinecraftControls
import me.anno.minecraft.ui.controls.MinecraftControls.Companion.inventorySizeX
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import org.joml.Vector3i

open class InventoryBlock(
    typeUUID: String, color: Int,
    texId: Int, nameDesc: NameDesc,
    val numSlots: Int
) : BlockType(typeUUID, color, texId, nameDesc), RightClickBlock {

    fun getOrCreateInventory(metadata: Metadata): Inventory {
        var inventory = metadata["inventory"] as? Inventory
        if (inventory != null) return inventory

        inventory = Inventory(numSlots)
        metadata["inventory"] = inventory
        return inventory
    }

    open fun createInventoryUI(inventory: Inventory): Panel {
        val sx = inventorySizeX
        val inventoryBar = PanelListY(style)
        for (i in 0 until ceilDiv(numSlots, sx)) {
            val list = PanelListX(style)
            for (j in 0 until sx) {
                val index = i * sx + j
                if (index >= numSlots) break
                list.add(ItemPanel(inventory[index], index))
            }
            inventoryBar.add(list)
        }
        return inventoryBar
    }

    override fun onRightClick(controls: MinecraftControls, coords: Vector3i) {
        val metadata = dimension.getOrCreateMetadataAt(coords.x, coords.y, coords.z)
        val inventory = getOrCreateInventory(metadata)
        controls.openInventory(createInventoryUI(inventory))
    }

}