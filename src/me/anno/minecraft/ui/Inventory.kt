package me.anno.minecraft.ui

import me.anno.utils.structures.lists.Lists.createList

class Inventory(numSlots: Int) {
    val slots = createList(numSlots) {
        ItemSlot()
    }
}