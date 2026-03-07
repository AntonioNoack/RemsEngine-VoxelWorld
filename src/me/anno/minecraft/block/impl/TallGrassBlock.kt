package me.anno.minecraft.block.impl

import me.anno.language.translation.NameDesc
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.builder.BlockBuilder
import me.anno.minecraft.block.types.CustomBlockBounds
import me.anno.minecraft.block.types.DetailedBlockVisuals
import me.anno.minecraft.ui.Utils.classLoader
import me.anno.utils.Color.a
import org.joml.AABBf
import javax.imageio.ImageIO

class TallGrassBlock(typeUUID: String, color: Int, nameDesc: NameDesc) :
    BlockType(typeUUID, color, 226, nameDesc), DetailedBlockVisuals, CustomBlockBounds {

    // todo make walk-through-able

    companion object {
        private val bounds = AABBf(0f, 0f, 0f, 1f, 1f / 16f, 1f)
        private val grassTexture = ImageIO.read(classLoader.getResourceAsStream("textures/blocks/GrassTexture.png"))
            ?: throw IllegalStateException("Missing GrassTexture")
        private val model = BlockBuilder()
            .apply {
                val texId = 226
                for (x in 0 until grassTexture.width) {
                    var y = 0
                    while (y < grassTexture.height) {
                        val y0 = y
                        while (y < grassTexture.height && grassTexture.getRGB(x, y).a() > 150) y++
                        val h = y - y0
                        if (h > 0) {
                            addCube(x, 16 - y, x, 1, h, 1, texId)
                            addCube(x, 16 - y, 15 - x, 1, h, 1, texId)
                        } else y++
                    }
                }
            }
            .build()
    }

    override val customSize: AABBf get() = bounds
    override fun getModel() = model
}