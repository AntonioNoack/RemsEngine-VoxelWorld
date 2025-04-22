package me.anno.minecraft.ui

import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.item.ItemType

class ItemSlot(
    type: ItemType, var count: Int,
    var metadata: Metadata?
) {

    constructor() : this(BlockRegistry.Air, 0, null)

    var type = type
        private set

    fun set(type: ItemType, count: Int, metadata: Metadata?) {
        this.type = type
        this.count = count
        this.metadata = metadata
    }

}