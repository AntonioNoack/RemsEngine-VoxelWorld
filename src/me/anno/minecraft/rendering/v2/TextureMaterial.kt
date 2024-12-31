package me.anno.minecraft.rendering.v2

import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.minecraft.rendering.v2.BlockTexture.texture

class TextureMaterial : Material() {

    init {
        shader = TextureShader
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        val tex = if (texture.isCreated()) texture else TextureLib.whiteTex3d
        tex.bind(shader, "diffuseMapArray", Filtering.NEAREST, Clamping.REPEAT)
    }

    companion object {
        val solid = TextureMaterial()
        val fluid = TextureMaterial().apply { pipelineStage = PipelineStage.TRANSPARENT }
    }
}