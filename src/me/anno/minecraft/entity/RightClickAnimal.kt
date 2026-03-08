package me.anno.minecraft.entity

import me.anno.minecraft.ui.ItemSlot
import me.anno.minecraft.ui.controls.MinecraftControls

interface RightClickAnimal {
    fun onRightClick(controls: MinecraftControls, inHand: ItemSlot)
}