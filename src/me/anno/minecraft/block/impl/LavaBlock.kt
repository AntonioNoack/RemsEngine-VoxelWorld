package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.block.types.EffectBlock
import me.anno.minecraft.block.types.FluidBlock
import me.anno.minecraft.entity.effect.Effect
import me.anno.minecraft.entity.effect.EffectType
import me.anno.minecraft.world.Chunk

class LavaBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), FluidBlock, EffectBlock {

    companion object {
        private val effects = listOf(Effect(EffectType.BURNING, 3, 10f))
    }

    // todo show effects in UI
    override fun getEffects(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk) = effects

}