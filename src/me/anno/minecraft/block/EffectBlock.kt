package me.anno.minecraft.block

import me.anno.minecraft.entity.Animal
import me.anno.minecraft.entity.effect.Effect
import me.anno.minecraft.world.Chunk

interface EffectBlock {
    fun getEffects(
        x: Int, y: Int, z: Int, metadata: Metadata?,
        chunk: Chunk,
    ): List<Effect>
}