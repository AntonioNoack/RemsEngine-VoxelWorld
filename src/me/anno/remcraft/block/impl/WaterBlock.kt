package me.anno.remcraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.types.FluidBlock

class WaterBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), FluidBlock