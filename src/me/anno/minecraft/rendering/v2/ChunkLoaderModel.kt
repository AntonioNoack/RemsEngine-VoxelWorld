package me.anno.minecraft.rendering.v2

import me.anno.mesh.vox.model.VoxelModel
import me.anno.minecraft.world.Chunk

class ChunkLoaderModel(val chunk: Chunk) : VoxelModel(csx, csy, csz) {
    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlock(x, y, z).id.toInt()
    }
}