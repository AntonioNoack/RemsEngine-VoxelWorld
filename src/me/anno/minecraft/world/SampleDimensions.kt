package me.anno.minecraft.world

import me.anno.minecraft.block.BlockType
import me.anno.minecraft.world.decorator.CactiDecorator
import me.anno.minecraft.world.decorator.PyramidDecorator
import me.anno.minecraft.world.decorator.TreeDecorator
import me.anno.minecraft.world.generator.Perlin3dWorldGenerator
import me.anno.minecraft.world.generator.PerlinWorldGenerator

object SampleDimensions {

    val decorators = listOf(
        TreeDecorator(0.03f, 5123L),
        PyramidDecorator(BlockType.Sandstone, 10, BlockType.Sand, 0.00001f, 49651L),
        PyramidDecorator(BlockType.Sandstone, 20, BlockType.Sand, 0.00001f / 3f, 19651L),
        PyramidDecorator(BlockType.Sandstone, 27, BlockType.Sand, 0.00001f / 9f, 29651L),
        CactiDecorator(0.001f, 97845L)
    )

    val perlin2dDim = Dimension(
        PerlinWorldGenerator(
            listOf(BlockType.Stone, BlockType.Dirt, BlockType.Dirt, BlockType.Dirt, BlockType.Grass),
            BlockType.Water, 30, 0.02f, 0f, 100f, 1234L
        ),
        decorators
    )

    val perlin3dDim = Dimension(
        Perlin3dWorldGenerator(
            listOf(BlockType.Stone, BlockType.Dirt, BlockType.Dirt, BlockType.Dirt, BlockType.Grass),
            listOf(BlockType.Stone, BlockType.Sandstone, BlockType.Sand, BlockType.Sand),
            1234L
        ),
        decorators
    )

    val sandDim = Dimension(
        PerlinWorldGenerator(
            listOf(BlockType.Stone, BlockType.Sand, BlockType.Sand),
            BlockType.Stone, 5, 0.015f, 0f, 30f, 5123L
        ),
        decorators
    )

}