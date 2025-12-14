package me.anno.minecraft.rendering.v2

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.mesh.vox.model.VoxelModel
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.world.Chunk
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Floats.roundToIntOr
import java.nio.ByteBuffer

class ChunkLoaderModel(val chunk: Chunk) : VoxelModel(csx, csy, csz) {
    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return chunk.getBlock(x, y, z).id.toInt()
    }

    fun createMesh(palette: IntArray, blockFilter: BlockFilter): Mesh {
        center0()
        return createMesh(palette, { x, y, z ->
            val block = dimension.getBlockAt(
                x + chunk.x0,
                y + chunk.y0,
                z + chunk.z0
            ) ?: BlockRegistry.Air
            block.id.toInt().and(0xffff)
        }, { inside, outside ->
            val inside1 = BlockRegistry.byId[inside]
            val outside1 = BlockRegistry.byId[outside]
            blockFilter(inside1, outside1)
        })
    }

    fun createBuffer(palette: IntArray, blockFilter: BlockFilter): ByteBuffer {
        val mesh = createMesh(palette, blockFilter)
        return createBuffer(chunk.x0, chunk.y0, chunk.z0, mesh)
    }

    companion object {
        fun createBuffer(dx: Int, dy: Int, dz: Int, mesh: Mesh): ByteBuffer {
            val pos = mesh.positions!!
            val col = mesh.color0!!
            val numVertices = pos.size / 3
            val nio = ByteBufferPool.allocateDirect(numVertices * 4 * 2)
            for (i in 0 until numVertices) {
                val texId = col[i] - 1 // 0 is air
                val x = dx + pos[i * 3].roundToIntOr()
                val y = dy + pos[i * 3 + 1].roundToIntOr()
                val z = dz + pos[i * 3 + 2].roundToIntOr()
                nio.putShort(x.toShort())
                nio.putShort(y.toShort())
                nio.putShort(z.toShort())
                nio.putShort(texId.toShort())
            }
            return nio
        }
    }
}