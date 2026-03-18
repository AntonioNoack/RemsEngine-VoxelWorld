package me.anno.remcraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.Metadata
import me.anno.remcraft.block.types.EffectBlock
import me.anno.remcraft.block.types.FluidBlock
import me.anno.remcraft.entity.effect.StatusEffect
import me.anno.remcraft.entity.effect.EffectType
import me.anno.remcraft.world.Chunk

class LavaBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), FluidBlock, EffectBlock {

    companion object {
        private val effects = listOf(StatusEffect(EffectType.BURNING, 3, 10f))
    }

    // todo show effects in UI
    override fun getEffects(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk) = effects

}