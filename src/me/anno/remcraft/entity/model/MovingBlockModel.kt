package me.anno.remcraft.entity.model

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.remcraft.entity.ItemEntity

class MovingBlockModel(
    sx: Int, sy: Int, sz: Int,
    x0: Int, y0: Int,
    texSize: Int,
) : Model<ItemEntity>() {

    private val mesh = CuboidCreator.createMonoCuboid(sx, sy, sz, x0, y0, texSize)

    override fun fill(transform: Transform, callback: (IMesh, Transform) -> Unit) {
        callback(mesh, transform)
    }
}