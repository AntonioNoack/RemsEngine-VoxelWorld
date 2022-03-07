package me.anno.minecraft.world.decorator

import me.anno.minecraft.world.Chunk

abstract class Decorator {

    abstract fun decorate(chunk: Chunk)

}