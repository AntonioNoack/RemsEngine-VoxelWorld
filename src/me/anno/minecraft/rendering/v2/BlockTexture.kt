package me.anno.minecraft.rendering.v2

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCache
import me.anno.utils.OS.res
import me.anno.utils.async.promise

object BlockTexture {
    val texture = Texture2DArray("Blocks", 1, 1, 1)

    init {
        promise { cb ->
            ImageCache.getAsync(res.getChild("blocks.png"), ImageCache.timeoutMillis, true, cb)
        }.then { src ->
            val imageList = src.split(src.width / 16, src.height / 16)
            addGPUTask("Atlas", src.width, src.height) {
                texture.create(imageList, false)
            }
        }
    }
}