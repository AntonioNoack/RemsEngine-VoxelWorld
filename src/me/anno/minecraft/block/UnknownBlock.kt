package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.utils.Color.black

object UnknownBlock : BlockType(
    "unknown", 0xff00ff or black, 0,
    NameDesc("Unknown Block")
)