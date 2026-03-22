package me.anno.remcraft.rendering.globalillumination

import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.world.Chunk
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ

object ChunkFaces {

    fun interface ForEachFaceCallback {
        fun call(lx: Int, ly: Int, lz: Int, side: BlockSide)
    }

    fun Chunk.forEachFace(callback: ForEachFaceCallback) {
        for (lz in 0 until sizeZ) {
            for (ly in 0 until sizeY) {
                for (lx in 0 until sizeX) {
                    val selfSolid = getBlock(lx, ly, lz).isSolid
                    if (!selfSolid) continue
                    for (side in BlockSide.entries) {
                        val otherSolid = getBlockAround(lx + side.x, ly + side.y, lz + side.z)!!.isSolid
                        if (!otherSolid) {
                            callback.call(lx, ly, lz, side)
                        }
                    }
                }
            }
        }
    }

}