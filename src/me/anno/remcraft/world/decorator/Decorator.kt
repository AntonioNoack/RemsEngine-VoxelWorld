package me.anno.remcraft.world.decorator

import me.anno.remcraft.world.Chunk

interface Decorator {
    fun decorate(chunk: Chunk)
    val readsPreviousStage: Boolean
}