package me.anno.remcraft.item

import me.anno.remcraft.entity.PlayerEntity
import me.anno.remcraft.ui.ItemSlot
import org.joml.Vector3i

interface RightClickItem {
    fun onRightClickItem(player: PlayerEntity, slot: ItemSlot, coords: Vector3i?)
}