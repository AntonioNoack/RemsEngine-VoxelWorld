package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.world.Chunk
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import org.joml.AABBd

class WaterBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), FluidBlock