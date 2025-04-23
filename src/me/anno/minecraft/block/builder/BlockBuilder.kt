package me.anno.minecraft.block.builder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.minecraft.rendering.v2.TextureMaterial
import me.anno.utils.structures.arrays.ShortArrayList

/**
 * add cuboids, sample from the texture using texId,
 * and finally use that to create a traditional mesh
 * */
class BlockBuilder {

    private val data = ShortArrayList(64)

    fun addCube(
        px: Int, py: Int, pz: Int,
        sx: Int, sy: Int, sz: Int,
        texId: Int,
    ) {
        val baseShape = flatCube
        val pos = baseShape.positions!!
        baseShape.forEachTriangleIndex { ai, bi, ci ->
            addVertex(px, py, pz, sx, sy, sz, texId, pos, ai * 3)
            addVertex(px, py, pz, sx, sy, sz, texId, pos, bi * 3)
            addVertex(px, py, pz, sx, sy, sz, texId, pos, ci * 3)
            false
        }
    }

    private fun addVertex(
        px: Int, py: Int, pz: Int,
        sx: Int, sy: Int, sz: Int, texId: Int,
        values: FloatArray, ai: Int
    ) {
        val nx = if (values[ai] > 0f) px + sx else px
        val ny = if (values[ai + 1] > 0f) py + sy else py
        val nz = if (values[ai + 2] > 0f) pz + sz else pz
        addVertex(nx, ny, nz, texId)
    }

    private fun addVertex(px: Int, py: Int, pz: Int, texId: Int) {
        data.add(px.toShort())
        data.add(py.toShort())
        data.add(pz.toShort())
        data.add(texId.toShort())
    }

    fun build(): DetailedBlockMesh16 {
        val result = DetailedBlockMesh16()
        result.data = data.toShortArray()
        val material = TextureMaterial.solid
        result.materials = listOf(material.ref)
        return result
    }

    companion object {

        val spawnerModel by lazy {
            val builder = BlockBuilder()
            val texId = 16 + 13

            val vs = intArrayOf(0, 2, 5, 10, 13, 15)
            val ws = intArrayOf(0, 15)
            for (i in vs) {
                for (j in vs) {
                    if (i !in ws && j !in ws) continue
                    builder.addCube(0, i, j, 16, 1, 1, texId)
                    builder.addCube(i, 0, j, 1, 16, 1, texId)
                    builder.addCube(j, i, 0, 1, 1, 16, texId)
                }
            }

            builder.build()
        }

        fun slab(texId: Int, side: BlockSide): DetailedBlockMesh16 {
            val builder = BlockBuilder()
            builder.addCube(
                if (side.x == 1) 8 else 0,
                if (side.y == 1) 8 else 0,
                if (side.z == 1) 8 else 0,
                if (side.x == 0) 16 else 8,
                if (side.y == 0) 16 else 8,
                if (side.z == 0) 16 else 8,
                texId
            )
            return builder.build()
        }

        @JvmStatic
        fun main(args: Array<String>) {

            val builder = BlockBuilder()
            val lightWood = 12 * 16 + 3

            builder.addCube(0, 0, 0, 16, 1, 16, lightWood)

            val scene = Entity()
            Entity("Crafting Table", scene)
                .add(MeshComponent(builder.build().ref))
            Entity("Spawner", scene)
                .setPosition(-2.0, 0.0, 0.0)
                .add(MeshComponent(spawnerModel.ref))
            for (side in BlockSide.entries) {
                Entity("Slab[$side]", scene)
                    .setPosition(-2.0 + side.id * 1.2, 0.0, -1.2)
                    .add(MeshComponent(slab(7, side).ref))
            }
            testSceneWithUI("BlockBuilder", scene)
        }
    }

}