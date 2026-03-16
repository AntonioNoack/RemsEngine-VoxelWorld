package me.anno.remcraft.block.types

import org.joml.AABBf

// todo respect this when block-tracing
interface CustomBlockBounds {
    val customSize: AABBf
}