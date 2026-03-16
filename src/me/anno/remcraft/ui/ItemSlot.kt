package me.anno.remcraft.ui

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.Metadata
import me.anno.remcraft.item.ItemType
import me.anno.utils.types.AnyToInt.getInt

class ItemSlot(
    type: ItemType,
    var count: Int,
    var metadata: Metadata?
) : Saveable() {

    constructor() : this(BlockRegistry.Air, 0, null)

    var type = type

    fun set(type: ItemType, count: Int, metadata: Metadata?) {
        this.type = type
        this.count = count
        this.metadata = metadata
    }

    fun swap(other: ItemSlot) {
        val type = type
        val count = count
        val meta = metadata

        set(other.type, other.count, other.metadata)
        other.set(type, count, meta)
    }

    fun moveFrom(slot: ItemSlot, moved: Int) {
        if (slot === this) return

        count += moved
        type = slot.type
        metadata = slot.metadata

        slot.count -= moved
        if (slot.count == 0) {
            slot.type = BlockRegistry.Air
            slot.metadata = null
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("count", count)
        if (count > 0) {
            writer.writeString("type", type.typeUUID)
            writer.writeObject(this, "metadata", metadata)
        }
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "type" -> type = BlockRegistry.byUUID[value as? String] ?: BlockRegistry.Air
            "count" -> count = getInt(value)
            "metadata" -> metadata = value as? Metadata
            else -> super.setProperty(name, value)
        }
    }

    fun isNotEmpty(): Boolean = count > 0 && type != BlockRegistry.Air

    fun removeOne() {
        if (count < 2) {
            type = BlockRegistry.Air
            count = 0
            metadata = null
        } else {
            count--
        }
    }

}