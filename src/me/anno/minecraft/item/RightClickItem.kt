package me.anno.minecraft.item

import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.ui.ItemSlot
import org.joml.Vector3i

interface RightClickItem {
    fun onRightClickItem(player: PlayerEntity, slot: ItemSlot, coords: Vector3i?)
}