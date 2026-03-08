package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.minecraft.entity.ItemEntity

class MovingBlockModel(
    sx: Int, sy: Int, sz: Int,
    x0: Int, y0: Int,
    texSize: Int,
) : Model<ItemEntity>() {

    private val mesh = CuboidCreator.createMonoCuboid(sx, sy, sz, x0, y0, texSize)

    override fun fill(transform: Transform, callback: (Mesh, Transform) -> Unit) {
        callback(mesh, transform)
    }
}