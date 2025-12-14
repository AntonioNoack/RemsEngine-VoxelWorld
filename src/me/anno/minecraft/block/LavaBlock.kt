package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.minecraft.entity.effect.Effect
import me.anno.minecraft.entity.effect.EffectType
import me.anno.minecraft.world.Chunk

class LavaBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), FluidBlock, EffectBlock {

    companion object {
        private val effects = listOf(
            Effect(EffectType.BURNING, 3, 10f)
        )
    }

    override fun getEffects(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk) = effects

}