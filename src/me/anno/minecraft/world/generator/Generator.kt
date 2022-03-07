package me.anno.minecraft.world.generator

import me.anno.minecraft.world.Chunk
import me.anno.minecraft.world.decorator.Decorator

abstract class Generator {

    abstract fun generate(chunk: Chunk)

}