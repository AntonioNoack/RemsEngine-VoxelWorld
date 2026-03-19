package me.anno.remcraft.rendering.v1

import me.anno.mesh.vox.model.VoxelModel
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ

class ChunkVoxelModel(val chunk: Chunk) : VoxelModel(sizeX, sizeY, sizeZ) {
    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlockId(x, y, z).toInt().and(0xffff)
    }
}