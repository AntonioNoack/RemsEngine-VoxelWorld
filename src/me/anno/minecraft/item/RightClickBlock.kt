package me.anno.minecraft.item

import me.anno.minecraft.ui.controls.MinecraftControls
import org.joml.Vector3i

interface RightClickBlock {
    fun onRightClickBlock(controls: MinecraftControls, coords: Vector3i)
}