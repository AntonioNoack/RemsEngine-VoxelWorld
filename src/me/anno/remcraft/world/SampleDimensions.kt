package me.anno.remcraft.world

import me.anno.remcraft.block.BlockRegistry.DiamondOre
import me.anno.remcraft.block.BlockRegistry.Dirt
import me.anno.remcraft.block.BlockRegistry.GoldOre
import me.anno.remcraft.block.BlockRegistry.Grass
import me.anno.remcraft.block.BlockRegistry.IronOre
import me.anno.remcraft.block.BlockRegistry.Sand
import me.anno.remcraft.block.BlockRegistry.Sandstone
import me.anno.remcraft.block.BlockRegistry.Stone
import me.anno.remcraft.block.BlockRegistry.Water
import me.anno.remcraft.world.decorator.CactiDecorator
import me.anno.remcraft.world.decorator.OreDecorator
import me.anno.remcraft.world.decorator.PyramidDecorator
import me.anno.remcraft.world.decorator.TreeDecorator
import me.anno.remcraft.world.generator.Perlin3dWorldGenerator
import me.anno.remcraft.world.generator.PerlinWorldGenerator

object SampleDimensions {

    val decorators = listOf(
        TreeDecorator(0.03f, 5123L),
        PyramidDecorator(Sandstone, 10, Sand, 0.00001f, 49651L),
        PyramidDecorator(Sandstone, 20, Sand, 0.00001f / 3f, 19651L),
        PyramidDecorator(Sandstone, 27, Sand, 0.00001f / 9f, 29651L),
        CactiDecorator(0.001f, 97845L),
        OreDecorator(0.0001f)
            .addOreType(IronOre, 10f)
            .addOreType(GoldOre, 3f)
            .addOreType(DiamondOre, 1f)
    )

    val perlin2dDim = Dimension(
        PerlinWorldGenerator(
            listOf(Stone, Dirt, Dirt, Dirt, Grass),
            Water, 30, 0.02f, 0f, 100f, 1234L
        ),
        decorators
    )

    val perlin3dDim = Dimension(
        Perlin3dWorldGenerator(
            listOf(Stone, Dirt, Dirt, Dirt, Grass),
            listOf(Stone, Sandstone, Sand, Sand),
            1234L
        ),
        decorators
    )

    val sandDim = Dimension(
        PerlinWorldGenerator(
            listOf(Stone, Sand, Sand),
            Stone, 5, 0.015f, 0f, 30f, 5123L
        ),
        decorators
    )

}