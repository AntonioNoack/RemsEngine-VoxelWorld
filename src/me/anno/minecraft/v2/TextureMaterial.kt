package me.anno.minecraft.v2

import me.anno.ecs.components.mesh.Material
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference

object TextureMaterial : Material() {

    private val texture: Texture2DArray

    init {
        shader = TextureShader
        val src = ImageCPUCache[getReference("res://blocks.png"), false]!!
        val data = src.split(src.width / 16, src.height / 16)
        texture = Texture2DArray("Blocks", 16, 16, data.size).apply {
            create(data, false)
        }
    }

    override fun bind(shader: Shader) {
        super.bind(shader)
        texture.bind(shader, "diffuseMapArray", GPUFiltering.NEAREST, Clamping.REPEAT)
    }

}