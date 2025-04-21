package me.anno.minecraft.block

import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.minecraft.item.ItemType
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha

open class BlockType(
    val color: Int, val id: Short, texId: Int,
    nameDesc: NameDesc
) : ItemType(InvalidRef, texId, nameDesc) {

    val isSolid get() = color.a() == 255
    val isFluid get() = color.a() in 1 until 255

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        val trans = black.withAlpha(0x7f)

        val Air = AirBlock
        val Stone = BlockType(black or 0x778899, 1, 48, NameDesc("Stone"))
        val Grass = BlockType(black or 0x55aa33, 2, 226, NameDesc("Grass"))
        val Dirt = BlockType(black or 0x997755, 3, 112, NameDesc("Dirt"))
        val Water = BlockType(trans or 0x3344ff, 4, 8 * 16 + 2, NameDesc("Water"))
        val Lava = BlockType(trans or 0xd97520, 5, 9 * 16 + 7, NameDesc("Lava"))
        val Log = BlockType(black or 0x835127, 6, 189, NameDesc("Log"))
        val Leaves = BlockType(black or 0x187423, 7, 240, NameDesc("Leaves"))
        val Sand = BlockType(black or 0xeddc9e, 8, 7 * 16 + 2, NameDesc("Sand"))
        val Sandstone = BlockType(black or 0x9f946b, 9, 185, NameDesc("Sandstone"))
        val Cactus = BlockType(black or 0x77975a, 10, 13 * 16 + 15, NameDesc("Cactus"))

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