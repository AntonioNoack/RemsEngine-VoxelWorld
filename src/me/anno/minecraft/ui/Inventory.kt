package me.anno.minecraft.ui

class Inventory(numSlots: Int) {
    val slots = List(numSlots) {
        ItemSlot()
    }
}