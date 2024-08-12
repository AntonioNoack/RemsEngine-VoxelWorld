package me.anno.minecraft.ui

import me.anno.minecraft.block.BlockType
import me.anno.utils.structures.lists.Lists.createList

class Inventory(numSlots: Int) {
    val slots = createList(numSlots) {
        ItemSlot(BlockType.Air, 0)
    }
}