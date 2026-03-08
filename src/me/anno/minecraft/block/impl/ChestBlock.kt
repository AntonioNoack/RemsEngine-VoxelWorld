package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.builder.BlockBuilder
import me.anno.minecraft.block.builder.DetailedBlockMesh16
import me.anno.minecraft.block.types.CustomBlockBounds
import me.anno.minecraft.block.types.DetailedBlockVisuals
import org.joml.AABBf

// todo four different rotations
// todo sticking two chests together
class ChestBlock(typeUUID: String, color: Int, texId: Int, nameDesc: NameDesc) :
    BlockWithInventory(typeUUID, color, texId, nameDesc, 9 * 3), CustomBlockBounds, DetailedBlockVisuals {

    companion object {
        private val bounds = AABBf(1f / 16f, 0f, 1f / 16, 15f / 16f, 14f / 16f, 15f / 16f)
        private val model = BlockBuilder().addCube(
            1, 0, 1, 14, 14, 14,
            18 * 16 + 12,
        ).build()
    }

    override val customSize: AABBf
        get() = bounds

    override fun getModel(): DetailedBlockMesh16 = model

    // todo custom model,
    // todo opening and closing animation using entity

}