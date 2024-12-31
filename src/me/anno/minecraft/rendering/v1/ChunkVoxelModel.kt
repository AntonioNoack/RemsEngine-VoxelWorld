package me.anno.minecraft.rendering.v1

import me.anno.mesh.vox.model.VoxelModel
import me.anno.minecraft.world.Chunk

class ChunkVoxelModel(val chunk: Chunk) : VoxelModel(chunk.dimension.sizeX, chunk.dimension.sizeY, chunk.dimension.sizeZ) {
    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlock(x, y, z).id.toInt().and(0xffff)
    }
}