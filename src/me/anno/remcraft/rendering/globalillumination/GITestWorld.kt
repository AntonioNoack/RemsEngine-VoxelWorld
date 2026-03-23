package me.anno.remcraft.rendering.globalillumination

import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Dimension
import me.anno.remcraft.world.SampleDimensions


fun createWorld(
    simpleWorld: Boolean,
    callback: (Chunk) -> Unit
): Dimension {
    val dimension =
        if (simpleWorld) Dimension(emptyList())
        else SampleDimensions.perlin2dDim

    if (simpleWorld) {
        fun set(x: Int, y: Int, z: Int, type: BlockType) {
            dimension.getChunkAt(x, y, z)!!
                .setBlockQuickly(x, y, z, type.id)
        }

        fun fill(x: Int, y: Int, z: Int, sx: Int, sy: Int, sz: Int, type: BlockType) {
            for (dx in 0 until sx) {
                for (dy in 0 until sy) {
                    for (dz in 0 until sz) {
                        set(x + dx, y + dy, z + dz, type)
                    }
                }
            }
        }

        fill(-9, 0, -9, 19, 1, 19, BlockRegistry.Stone)
        fill(-7, 3, -7, 15, 1, 15, BlockRegistry.Stone)

        fill(-9, 32, -9, 19, 1, 19, BlockRegistry.Stone)
        fill(-7, 35, -7, 15, 1, 15, BlockRegistry.Stone)
    }

    val dx = 1
    val dz = 1
    val y0 = if (simpleWorld) 0 else 1
    val y1 = if (simpleWorld) 1 else 2

    for (xi in -dx..dx) {
        for (zi in -dz..dz) {
            for (yi in y0..y1) {
                val chunk = dimension.getChunk(xi, yi, zi, Int.MAX_VALUE)
                callback(chunk.waitFor()!!)
            }
        }
    }

    return dimension

}