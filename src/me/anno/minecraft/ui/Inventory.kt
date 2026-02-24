package me.anno.minecraft.ui

import kotlin.math.min

class Inventory(numSlots: Int) {

    val slots = List(numSlots) {
        ItemSlot()
    }

    fun addItemFrom(stack: ItemSlot): Boolean {
        val oldCount = stack.count
        for (i in slots.indices) {
            val slot = slots[i]
            if ((slot.type == stack.type && slot.metadata == stack.metadata) || slot.count == 0) {
                val transferred = min(stack.type.stackingLimit - slot.count, stack.count)
                if (transferred > 0) {
                    slot.type = stack.type
                    slot.metadata = stack.metadata
                    slot.count += transferred
                    stack.count -= transferred
                    if (stack.count == 0) return true
                }
            }
        }
        return stack.count != oldCount
    }
}