package me.anno.remcraft.item

import me.anno.remcraft.ui.controls.RemcraftControls
import org.joml.Vector3i

interface RightClickBlock {
    fun onRightClickBlock(controls: RemcraftControls, coords: Vector3i)
}