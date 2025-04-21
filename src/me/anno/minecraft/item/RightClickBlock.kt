package me.anno.minecraft.item

import me.anno.minecraft.ui.MinecraftControls
import org.joml.Vector3i

interface RightClickBlock {
    fun onRightClick(controls: MinecraftControls, coords: Vector3i)
}