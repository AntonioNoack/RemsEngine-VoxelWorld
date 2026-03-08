package me.anno.minecraft.ui.controls

enum class GameMode {
    CREATIVE,
    SURVIVAL,
    ADVENTURE,
    SPECTATOR;

    fun canBeAttacked() = this == SURVIVAL || this == ADVENTURE
    fun canFly() = this == CREATIVE || this == SPECTATOR

    fun canInstantlyMine() = this == CREATIVE
    fun canSlowlyMine() = this == SURVIVAL

    fun alwaysFlying() = this == SPECTATOR
    fun canPickBlocks() = this == CREATIVE
    fun canPlaceBlocks() = this == SURVIVAL || this == CREATIVE

    fun isGhost() = this == SPECTATOR

    fun getReachDistance(): Double = when (this) {
        CREATIVE, SPECTATOR -> 10.0
        SURVIVAL, ADVENTURE -> 3.5
    }

    fun finiteInventory() = this != CREATIVE
}