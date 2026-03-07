package me.anno.minecraft.ui.controls

enum class GameMode {
    CREATIVE,
    SURVIVAL,
    ADVENTURE,
    SPECTATOR;

    fun canBeAttacked() = this == SURVIVAL || this == ADVENTURE
    fun canFly() = this == CREATIVE || this == SPECTATOR
    fun canInstantlyMine() = this == CREATIVE
    fun alwaysFlying() = this == SPECTATOR
    fun canPickBlocks() = this == CREATIVE
    fun getReachDistance(): Double = when (this) {
        CREATIVE, SPECTATOR -> 10.0
        SURVIVAL, ADVENTURE -> 3.5
    }
}