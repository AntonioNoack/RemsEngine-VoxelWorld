package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.minecraft.rendering.v2.dimension
import me.anno.minecraft.world.Chunk
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import org.joml.AABBd

class CactusBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, texId, nameDesc), ChangingBlock {

    companion object {
        private val checkedSides = listOf(
            BlockSide.NX,
            BlockSide.PX,
            BlockSide.NZ,
            BlockSide.PZ,
        )
        private val selfBounds = AABBd()
            .setMin(-0.2, 0.01, -0.2)
            .setMax(+1.2, 0.99, +1.2)
    }

    fun collidesWithAnyNeighbor(x: Int, y: Int, z: Int): Boolean {
        val tmp = JomlPools.aabbd.create()
        val shouldBreak = checkedSides.any2 { side ->
            val other = dimension.getBlockAt(x, y, z, side)
            other != null && other.isSolid && other.getBounds(side.x, side.y, side.z, tmp).testAABB(selfBounds)
            false
        }
        JomlPools.aabbd.sub(1)
        return shouldBreak
    }

    override fun onBlockUpdate(x: Int, y: Int, z: Int, metadata: Metadata?, chunk: Chunk) {
        // todo jump away from breaking block?
        if (collidesWithAnyNeighbor(x, y, z)) {
            dropAsItem(x, y, z, metadata)
        }
    }
}