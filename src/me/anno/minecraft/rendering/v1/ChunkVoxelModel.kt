package me.anno.minecraft.rendering.v1

import me.anno.mesh.vox.model.VoxelModel
import me.anno.minecraft.world.Chunk
import me.anno.minecraft.world.Dimension

class ChunkVoxelModel(val chunk: Chunk, dimension: Dimension = chunk.dimension) :
    VoxelModel(dimension.sizeX, dimension.sizeY, dimension.sizeZ) {

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlockId(x, y, z).toInt().and(0xffff)
    }
}