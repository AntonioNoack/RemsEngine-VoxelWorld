package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import me.anno.minecraft.entity.ItemEntity
import me.anno.minecraft.entity.Texture

class MovingBlockModel(
    sx: Int, sy: Int, sz: Int,
    x0: Int, y0: Int,
    texture: Texture
) : Model<ItemEntity>() {

    private val mesh = CuboidCreator.createMonoCuboid(sx, sy, sz, x0, y0, texture)

    override fun fill(pipeline: Pipeline, transform: Transform) {
        pipeline.addMesh(mesh, self, transform)
    }
}