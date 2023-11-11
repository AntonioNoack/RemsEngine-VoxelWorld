package me.anno.minecraft.block

import me.anno.utils.Color.black

open class BlockType(val color: Int, val id: Short) {

    val isTransparent get() = this == Air

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        val Air = BlockType(0, 0)
        val Stone = BlockType(black or 0x778899, 1)
        val Grass = BlockType(black or 0x55aa33, 2)
        val Dirt = BlockType(black or 0x997755, 3)
        val Water = BlockType(black or 0x3344ff, 4)
        val Lava = BlockType(black or 0xd97520, 5)
        val Log = BlockType(black or 0x835127, 6)
        val Leaves = BlockType(black or 0x187423, 7)
        val Sand = BlockType(black or 0xeddc9e, 8)
        val Sandstone = BlockType(black or 0x9f946b, 9)
        val Cactus = BlockType(black or 0x77975a, 10)

        val library = hashSetOf(
            Air, Stone, Grass, Dirt, Water, Lava, Log, Leaves, Sand, Sandstone,
            Cactus
        )

        val byId = HashMap(library.associateBy { it.id })

        // for mods
        fun register(blockType: BlockType) {
            library.add(blockType)
            byId[blockType.id] = blockType
        }

    }

}