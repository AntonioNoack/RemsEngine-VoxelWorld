package me.anno.remcraft.rendering.v1

import me.anno.mesh.vox.model.VoxelModel
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Dimension

class ChunkVoxelModel(val chunk: Chunk, dimension: Dimension = chunk.dimension) :
    VoxelModel(dimension.sizeX, dimension.sizeY, dimension.sizeZ) {

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlockId(x, y, z).toInt().and(0xffff)
    }
}