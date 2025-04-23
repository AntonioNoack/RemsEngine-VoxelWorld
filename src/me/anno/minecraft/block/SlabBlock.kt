package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.minecraft.block.builder.BlockBuilder
import me.anno.utils.structures.lists.LazyList
import org.joml.AABBf

class SlabBlock(val blockType: BlockType, val side: BlockSide) : BlockType(
    "${blockType.typeUUID}.slab[${side.id}]", blockType.color,
    blockType.texId, NameDesc("${blockType.nameDesc.name} Slab")
), CustomBlockBounds, DetailedBlockVisuals {

    override val customSize: AABBf = slabSizes[side]!!

    override fun getModel() = modelInstance
    private val modelInstance = slabModels[blockType.texId * 6 + side.ordinal]

    companion object {

        val slabSizes = mapOf(
            BlockSide.NY to AABBf(0f, 0f, 0f, 1f, 0.5f, 1f),
            BlockSide.PY to AABBf(0f, 0.5f, 0f, 1f, 1f, 1f),
        )

        val slabModels = LazyList(6 * 65536) { id: Int ->
            val texId = id / 6
            val side = BlockSide.entries[id % 6]
            BlockBuilder.slab(texId, side)
        }

    }
}