package me.anno.remcraft.item

interface DurableItem {
    companion object {
        const val METADATA_DURABILITY = "durability"
    }

    val maxHealth: Int
}