package me.anno.minecraft.ui.controls

enum class GameMode {
    CREATIVE,
    SURVIVAL,
    ADVENTURE,
    SPECTATOR;

    fun canBeAttacked() = this == SURVIVAL || this == ADVENTURE
}