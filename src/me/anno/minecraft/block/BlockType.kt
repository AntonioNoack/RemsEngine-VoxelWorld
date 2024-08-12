package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.utils.Color.black

open class BlockType(
    val color: Int, val id: Short, val texId: Int,
    val nameDesc: NameDesc
) {

    val isTransparent get() = this == Air

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        val Air = BlockType(0, 0, -1, NameDesc("Air"))
        val Stone = BlockType(black or 0x778899, 1, 48, NameDesc("Stone"))
        val Grass = BlockType(black or 0x55aa33, 2, 226, NameDesc("Grass"))
        val Dirt = BlockType(black or 0x997755, 3, 112, NameDesc("Dirt"))
        val Water = BlockType(black or 0x3344ff, 4, -1, NameDesc("Water"))
        val Lava = BlockType(black or 0xd97520, 5, -1, NameDesc("Lava"))
        val Log = BlockType(black or 0x835127, 6, 189, NameDesc("Log"))
        val Leaves = BlockType(black or 0x187423, 7, 240, NameDesc("Leaves"))
        val Sand = BlockType(black or 0xeddc9e, 8, -1, NameDesc("Sand"))
        val Sandstone = BlockType(black or 0x9f946b, 9, 185, NameDesc("Sandstone"))
        val Cactus = BlockType(black or 0x77975a, 10, -1, NameDesc("Cactus"))

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

    override fun toString(): String {
        return nameDesc.name
    }

}