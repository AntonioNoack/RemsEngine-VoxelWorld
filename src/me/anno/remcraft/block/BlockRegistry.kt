package me.anno.remcraft.block

import me.anno.language.translation.NameDesc
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockColor.NUM_TEX_X
import me.anno.remcraft.block.impl.*
import me.anno.remcraft.block.shapes.DoubleSlabBlock
import me.anno.remcraft.block.shapes.FenceBlock
import me.anno.remcraft.block.shapes.LayerBlock
import me.anno.remcraft.block.shapes.SlabBlock
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import kotlin.test.assertTrue

object BlockRegistry {

    val trans = black.withAlpha(0x7f)

    val Air = AirBlock
    val Unknown = UnknownBlock
    val Stone = BlockType("remcraft.stone", black or 0x778899, 3 * NUM_TEX_X + 0, NameDesc("Stone"))

    val IronOre = BlockType("remcraft.iron.ore", black or 0x777777, 6 * NUM_TEX_X + 24, NameDesc("Iron Ore"))
        .apply { droppedXpOrbs = 3 /* todo drop special items */ }
    val GoldOre = BlockType("remcraft.gold.ore", black or 0xaaaa88, 6 * NUM_TEX_X + 22, NameDesc("Gold Ore"))
        .apply { droppedXpOrbs = 4 /* todo drop special items */ }
    val DiamondOre = BlockType("remcraft.diamond.ore", black or 0x77cccc, 6 * NUM_TEX_X + 21, NameDesc("Diamond Ore"))
        .apply { droppedXpOrbs = 5 /* todo drop special items */ }

    val Grass = BlockType("remcraft.grass", black or 0x55aa33, 14 * NUM_TEX_X + 2, NameDesc("Grass"))
    val TallGrass = TallGrassBlock("remcraft.grass.tall", black or 0x55aa44, 14 * NUM_TEX_X + 2, NameDesc("Tall Grass"))
    val Dirt = BlockType("remcraft.dirt", black or 0x997755, 7 * NUM_TEX_X + 0, NameDesc("Dirt"))

    val Snow = BlockType("remcraft.snow", black or 0xffffff, 17 * NUM_TEX_X + 12, NameDesc("Snow"))

    val Water = WaterBlock("remcraft.water", trans or 0x3344ff, 8 * NUM_TEX_X + 2, NameDesc("Water"))
    val Lava = LavaBlock("remcraft.lava", trans or 0xd97520, 9 * NUM_TEX_X + 7, NameDesc("Lava"))

    val Log = BlockType("remcraft.log", black or 0x835127, 11 * NUM_TEX_X + 13, NameDesc("Log"))
    val Leaves = BlockType("remcraft.leaves", black or 0x187423, 15 * NUM_TEX_X + 0, NameDesc("Leaves"))
    val Cactus = CactusBlock("remcraft.cactus", black or 0x77975a, 13 * NUM_TEX_X + 15, NameDesc("Cactus"))

    val Sand = SandBlock("remcraft.sand", black or 0xeddc9e, 7 * NUM_TEX_X + 2, NameDesc("Sand"))
    val Sandstone = BlockType("remcraft.sandstone", black or 0x9f946b, 11 * NUM_TEX_X + 9, NameDesc("Sandstone"))
    val Gravel = SandBlock("remcraft.gravel", black or 0x556060, 7 * NUM_TEX_X + 6, NameDesc("Gravel"))

    val Chest = ChestBlock("remcraft.chest", black or 0x9f946b, 18 * NUM_TEX_X + 12, NameDesc("Chest"))
    val Furnace = FurnaceBlock("remcraft.furnace", black or 0x333333, 3 * NUM_TEX_X + 11, NameDesc("Furnace"))
    val Hopper = HopperBlock("remcraft.hopper", black or 0x333339, 3 * NUM_TEX_X + 13, NameDesc("Hopper"))

    val SnowLayers = List(9) { depth ->
        when (depth) {
            0 -> Air
            in 1..7 -> LayerBlock(Snow, depth)
            else -> Snow
        }
    }

    val initialBlocks = listOf(
        Air, Unknown, Stone,
        Grass, Dirt, TallGrass, Snow,
        Water, Lava,
        Log, Leaves, Cactus,
        Sand, Sandstone, Gravel,
        Chest, Furnace, Hopper,
        IronOre, GoldOre, DiamondOre
    )

    val masonryBlocks = listOf(
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
            byUUID[blockType.typeUUID] = blockType
            blockType.id = id.toShort()
        }
    }

    init {
        for (block in initialBlocks) {
            register(block)
        }

        for (layer in SnowLayers) {
            if (layer is LayerBlock) register(layer)
        }

        for (block in masonryBlocks) {
            for (side in BlockSide.entries) {
                register(SlabBlock(block, side))
            }
            register(DoubleSlabBlock(block))
            register(FenceBlock(block))
        }
    }
}