package me.anno.minecraft.item

enum class MiningType {
    SWORD,
    PICKAXE,
    SHOVEL,
    AXE,
    HOE,
    OTHER;

    fun getMiningMultiplier(tool: MiningType): Float {
        if (this == tool) return if (this == OTHER) 2f else 1f
        if (tool == PICKAXE && this != SWORD) return 3f
        return 10f
    }
}