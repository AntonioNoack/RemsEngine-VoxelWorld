package me.anno.minecraft.item

import me.anno.minecraft.ui.MinecraftControls
import org.joml.Vector3i

interface RightClickItem {
    fun onRightClick(controls: MinecraftControls, coords: Vector3i?)
}