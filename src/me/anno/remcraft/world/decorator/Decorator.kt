package me.anno.remcraft.world.decorator

import me.anno.remcraft.world.Chunk

abstract class Decorator {

    abstract fun decorate(chunk: Chunk)

}