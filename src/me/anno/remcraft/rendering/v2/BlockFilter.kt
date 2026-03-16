package me.anno.remcraft.rendering.v2

import me.anno.remcraft.block.BlockType

typealias BlockFilter = (inner: BlockType, outer: BlockType) -> Boolean