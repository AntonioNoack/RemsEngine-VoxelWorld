package me.anno.minecraft.block

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.builder.BlockBuilder
import me.anno.utils.structures.lists.LazyList
import org.joml.AABBf

class FenceBlock(val blockType: BlockType) : BlockType(
    "${blockType.typeUUID}.fence", blockType.color,
    blockType.texId, NameDesc("${blockType.nameDesc.name} Slab")
), CustomBlockBounds, DetailedBlockVisuals {

    override val customSize: AABBf get() = fenceSize
    override fun getModel() = modelInstance
    private val modelInstance = fenceModel[texId]

    companion object {
        val fenceSize = AABBf(0f, 0f, 0f, 1f, 1.5f, 1f)

        val fenceModel = LazyList(65536) { texId ->
            val builder = BlockBuilder()
            builder.addCube(4, 0, 4, 8, 14, 8, texId)
            builder.addCube(4, 14, 5, 8, 2, 6, texId)
            builder.addCube(5, 14, 4, 6, 2, 8, texId)
            builder.addCube(0, 0, 5, 16, 12, 6, texId)
            builder.addCube(5, 0, 0, 6, 12, 16, texId)
            builder.build()
        }
    }
}