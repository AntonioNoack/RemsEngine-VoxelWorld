package me.anno.minecraft.rendering.v2

import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference
import me.anno.utils.Sleep

object TextureMaterial : Material() {

    private val texture = Texture2DArray("Blocks", 1, 1, 1)
    val hasTexture get() = texture.isCreated()

    init {
        shader = TextureShader
        Sleep.waitUntilDefined(true, {
            ImageCache[getReference("res://blocks.png"), true]
        }, { src ->
            val data = src.split(src.width / 16, src.height / 16)
            addGPUTask("Atlas", src.width, src.height) {
                texture.create(data, false)
            }
        })
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        val tex = if (texture.isCreated()) texture else TextureLib.whiteTex3d
        tex.bind(shader, "diffuseMapArray", Filtering.NEAREST, Clamping.REPEAT)
    }

    // todo this is randomly deleted :/, why??, by whom???
}