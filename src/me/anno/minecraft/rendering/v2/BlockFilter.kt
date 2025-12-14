package me.anno.minecraft.rendering.v2

import me.anno.minecraft.block.BlockType

typealias BlockFilter = (inner: BlockType, outer: BlockType) -> Boolean