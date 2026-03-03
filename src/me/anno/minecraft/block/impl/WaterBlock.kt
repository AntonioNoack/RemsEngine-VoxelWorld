package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.types.FluidBlock

class WaterBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), FluidBlock