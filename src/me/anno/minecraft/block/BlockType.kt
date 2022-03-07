package me.anno.minecraft.block

import me.anno.config.DefaultStyle.black

open class BlockType(val color: Int, val id: Short) {

    companion object {

        val Air = BlockType(0, 0)
        val Stone = BlockType(black or 0x778899, 1)
        val Grass = BlockType(black or 0x55aa33, 2)
        val Dirt = BlockType(black or 0x997755, 3)
        val Water = BlockType(black or 0x3344ff, 4)
        val Lava = BlockType(black or 0xd97520, 5)
        val Log = BlockType(black or 0x835127, 6)
        val Leaves = BlockType(black or 0x187423, 7)

        val library = hashSetOf(
            Air, Stone, Grass, Dirt, Water, Lava, Log, Leaves
        )

        val byId = HashMap(library.associateBy { it.id })

    }

}