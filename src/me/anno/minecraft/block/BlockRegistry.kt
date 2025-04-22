package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import kotlin.test.assertTrue

object BlockRegistry {

    val trans = black.withAlpha(0x7f)

    val Air = AirBlock
    val Unknown = UnknownBlock
    val Stone = BlockType("remcraft.stone", black or 0x778899, 48, NameDesc("Stone"))
    val Grass = BlockType("remcraft.grass", black or 0x55aa33, 226, NameDesc("Grass"))
    val Dirt = BlockType("remcraft.dirt", black or 0x997755, 112, NameDesc("Dirt"))
    val Water = BlockType("remcraft.water", trans or 0x3344ff, 8 * 16 + 2, NameDesc("Water"))
    val Lava = BlockType("remcraft.lava", trans or 0xd97520, 9 * 16 + 7, NameDesc("Lava"))
    val Log = BlockType("remcraft.log", black or 0x835127, 189, NameDesc("Log"))
    val Leaves = BlockType("remcraft.leaves", black or 0x187423, 240, NameDesc("Leaves"))
    val Sand = BlockType("remcraft.sand", black or 0xeddc9e, 7 * 16 + 2, NameDesc("Sand"))
    val Sandstone = BlockType("remcraft.sandstone", black or 0x9f946b, 185, NameDesc("Sandstone"))
    val Cactus = BlockType("remcraft.cactus", black or 0x77975a, 13 * 16 + 15, NameDesc("Cactus"))

    val initialBlocks = listOf(
        Air, Unknown, Stone, Grass, Dirt, Water, Lava, Log, Leaves, Sand, Sandstone,
        Cactus
    )

    val slabBlocks = listOf(
        Stone, Log, Sandstone,
    )

    val byId = ArrayList<BlockType>(64)
    val byUUID = HashMap<String, BlockType>(64)

    fun byId(id: Short): BlockType? {
        return byId.getOrNull(id.toInt().and(0xffff))
    }

    // for mods
    fun register(blockType: BlockType) {
        synchronized(BlockRegistry) {
            val id = byId.size
            assertTrue(id < 65536, "Too many blocks")
            byId.add(blockType)
            byUUID.put(blockType.typeUUID, blockType)
            blockType.id = id.toShort()
        }
    }

    init {

        // todo make IDs auto-increment, and always serialize a map of what which ID means using a name-ID
        //  (byID becomes an ArrayList, which saves allocations, and we avoid the hassle of ID overlaps)

        for (block in initialBlocks) {
            register(block)
        }

        for (block in slabBlocks) {
            register(SlabBlock(block, BlockSide.PY))
            register(SlabBlock(block, BlockSide.NY))
            register(DoubleSlabBlock(block))
        }
    }
}