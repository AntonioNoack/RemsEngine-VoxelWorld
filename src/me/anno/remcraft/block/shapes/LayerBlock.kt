package me.anno.remcraft.block.shapes

import me.anno.language.translation.NameDesc
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.builder.BlockBuilder
import me.anno.remcraft.block.types.CustomBlockBounds
import me.anno.remcraft.block.types.DetailedBlockVisuals
import org.joml.AABBf

class LayerBlock(val blockType: BlockType, val depth: Int) : BlockType(
    "${blockType.typeUUID}.layer[$depth]", blockType.color,
    blockType.texId, NameDesc("${blockType.nameDesc.name} Layer x$depth")
), CustomBlockBounds, DetailedBlockVisuals {

    override val customSize: AABBf get() = layerSize[depth - 1]

    private val modelI = BlockBuilder()
        .addCube(0, 0, 0, 16, depth * 2, 16, texId)
        .build()

    override fun getModel() = modelI

    companion object {
        val layerSize = List(7) { depth ->
            val y = 0.005f + depth * 0.125f
            AABBf(0f, 0f, 0f, 1f, y, 1f)
        }
    }
}