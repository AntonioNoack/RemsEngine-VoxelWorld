package me.anno.minecraft.item

import me.anno.maths.Maths.clamp
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.ui.ItemSlot
import me.anno.utils.assertions.assertEquals
import kotlin.math.pow

object Mining {

    fun getMiningDuration(target: BlockType, inHand: ItemSlot, miningSpeedEffectLevel: Int): Float {
        val durability = getRelativeDurability(inHand)
        return miningHardness(target, inHand) * durabilityMultiplier(durability) * effectMultiplier(
            miningSpeedEffectLevel
        )
    }

    private fun getRelativeDurability(inHand: ItemSlot): Float {
        return 0.5f
    }

    private fun miningHardness(target: BlockType, inHand: ItemSlot): Float {
        val inHandType = inHand.type
        val isUsingBlock = inHandType is BlockType
        val toolType = if (isUsingBlock) MiningType.OTHER else inHandType.miningType
        val extraMultiplier = if (isUsingBlock) 1f else 1f / inHandType.miningHardness
        return target.miningHardness * extraMultiplier * target.miningType.getMiningMultiplier(toolType)
    }

    private fun durabilityMultiplier(durability: Float): Float {
        // when low, it becomes harder and harder
        return 1f / (10f * clamp(durability) + 0.2f) + 0.9f
    }

    init {
        assertEquals(5.9f, durabilityMultiplier(0f), 0.1f)
        assertEquals(1.0f, durabilityMultiplier(1f), 0.1f)
    }

    private fun effectMultiplier(miningSpeedEffectLevel: Int): Float {
        return 1.3f.pow(miningSpeedEffectLevel)
    }

}