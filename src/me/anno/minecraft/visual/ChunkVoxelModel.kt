package me.anno.minecraft.visual

import me.anno.mesh.vox.model.VoxelModel
import me.anno.minecraft.world.Chunk

class ChunkVoxelModel(val chunk: Chunk) : VoxelModel(chunk.dim.sizeX, chunk.dim.sizeY, chunk.dim.sizeZ) {
    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlock(x, y, z).id.toInt().and(0xffff)
    }
}