package me.anno.minecraft.rendering.v2

import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCache
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager

object BlockTexture {

    private val LOGGER = LogManager.getLogger(BlockTexture::class)
    val texture = Texture2DArray("Blocks", 1, 1, 1)

    init {
        val srcFile = res.getChild("textures/blocks/blocks.png")
        LOGGER.info("Starting loading $srcFile as image")
        ImageCache[srcFile, ImageCache.timeoutMillis]
            .waitFor { src ->
                if (src != null) {
                    LOGGER.info("Loaded $src from $srcFile")
                    val imageList = src.split(src.width / 16, src.height / 16)
                    texture.create(imageList, false) { _, err ->
                        if (err == null) LOGGER.info("Created Atlas Texture from $srcFile, $src")
                        else err.printStackTrace()
                    }
                } else {
                    LOGGER.warn("Failed to load Atlas Texture from $srcFile")
                }
            }
    }
}