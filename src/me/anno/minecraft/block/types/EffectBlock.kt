package me.anno.minecraft.block.types

import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.effect.Effect
import me.anno.minecraft.world.Chunk

interface EffectBlock {
    fun getEffects(
        x: Int, y: Int, z: Int, metadata: Metadata?,
        chunk: Chunk,
    ): List<Effect>
}