package me.anno.remcraft.entity

import me.anno.remcraft.ui.ItemSlot
import me.anno.remcraft.ui.controls.RemcraftControls

interface RightClickAnimal {
    fun onRightClick(controls: RemcraftControls, inHand: ItemSlot)
}