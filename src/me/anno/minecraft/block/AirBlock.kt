package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import org.joml.AABBf

object AirBlock : BlockType(0, 0, -1, NameDesc("Air")), CustomBlockBounds {
    override val customSize: AABBf = AABBf()
}