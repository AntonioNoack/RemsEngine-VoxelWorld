package me.anno.remcraft.ui

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.image.Image
import me.anno.remcraft.ui.Utils.classLoader
import javax.imageio.ImageIO

object BreakModels {
    private val texture = ImageIO.read(classLoader.getResourceAsStream("textures/blocks/BreakTexture.png"))
    val cube = Mesh().apply {
        positions = floatArrayOf(
            -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f, 1f, -1f, -1f, -1f, -1f, -1f,
            -1f, 1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, -1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, -1f, -1f, -1f, 1f, 1f, -1f, 1f, -1f, 1f,
            -1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f, -1f, 1f, 1f, 1f, -1f, 1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f,
            -1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, -1f, -1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, 1f,
        )
        uvs = floatArrayOf(
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f,
            0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 0f,
            0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 1f,
        )
    }
    val materials = Array(10) { index ->
        Material().apply {
            val dx = index * 16
            linearFiltering = false
            diffuseMap = object : Image(16, 16, 4, true) {
                override fun setRGB(index: Int, value: Int) {}
                override fun getRGB(index: Int): Int = texture.getRGB(index.and(15) + dx, index.shr(4))
            }.ref
        }
    }
}