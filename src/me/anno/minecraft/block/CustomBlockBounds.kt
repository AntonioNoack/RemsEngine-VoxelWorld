package me.anno.minecraft.block

import org.joml.AABBf

// todo respect this when block-tracing
interface CustomBlockBounds {
    val customSize: AABBf
}