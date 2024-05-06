package me.anno.minecraft.v2

import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.GFX
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference
import me.anno.utils.Sleep

object TextureMaterial : Material() {

    private val texture = Texture2DArray("Blocks", 16, 16, 512)

    init {
        shader = TextureShader
        Sleep.waitUntilDefined(true, {
            ImageCache[getReference("res://blocks.png"), true]
        }, { src ->
            val data = src.split(src.width / 16, src.height / 16)
            GFX.addGPUTask("Atlas", src.width, src.height) {
                texture.create(data, false)
            }
        })
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        val tex = if (texture.isCreated()) texture else TextureLib.whiteTex3d
        tex.bind(shader, "diffuseMapArray", Filtering.NEAREST, Clamping.REPEAT)
    }

    override fun destroy() {
        super.destroy()
        texture.destroy()
    }
}