package me.anno.remcraft.block.types

import me.anno.remcraft.block.Metadata
import me.anno.remcraft.entity.effect.StatusEffect
import me.anno.remcraft.world.Chunk

interface EffectBlock {
    fun getEffects(
        x: Int, y: Int, z: Int, metadata: Metadata?,
        chunk: Chunk,
    ): List<StatusEffect>
}