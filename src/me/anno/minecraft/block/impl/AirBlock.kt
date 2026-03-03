package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.types.CustomBlockBounds
import org.joml.AABBf

object AirBlock : BlockType("remcraft.air",0, -1, NameDesc("Air")), CustomBlockBounds {
    override val customSize: AABBf = AABBf()
}