package me.anno.remcraft.coasters

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.maths.Maths.PIf
import me.anno.remcraft.block.builder.BlockBuilder
import me.anno.remcraft.entity.MovingEntity.Companion.place
import me.anno.remcraft.entity.model.Model
import kotlin.math.abs

object MinecartModel : Model<Minecart>() {

    @Suppress("KotlinConstantConditions")
    val minecartMesh = BlockBuilder()
        .apply {
            val texId = 3 * 16
            val dx = -8
            val dy = -5
            addCube(3 + dx, 1 + dy, 1 + dx, 1, 9, 14, texId)
            addCube(12 + dx, 1 + dy, 1 + dx, 1, 9, 14, texId)

            addCube(2 + dx, 5 + dy, 3 + dx, 1, 5, 10, texId)
            addCube(13 + dx, 5 + dy, 3 + dx, 1, 5, 10, texId)

            addCube(4 + dx, 1 + dy, 0 + dx, 8, 9, 2, texId)
            addCube(4 + dx, 1 + dy, 14 + dx, 8, 9, 2, texId)
            addCube(4 + dx, 0 + dy, 1 + dx, 8, 2, 14, texId)
        }
        .build()

    val wheelMesh = BlockBuilder()
        .apply {
            val texId = 3 * 16 + 11
            addCube(0, +1, -1, 1, 1, 2, texId)
            addCube(0, -1, -2, 1, 2, 4, texId)
            addCube(0, -2, -1, 1, 1, 2, texId)
        }
        .build()

    override fun fill(
        transform: Transform,
        callback: (IMesh, Transform) -> Unit
    ) {
        callback(minecartMesh, transform)

        val s = self.steering
        val x = self.animPosition
        val q = 6.2f + 1.5f * abs(s)
        callback(wheelMesh, getTransform(0).place(+q, -3f, +3f, -x, -s + PIf, 0f, null))
        callback(wheelMesh, getTransform(1).place(-q, -3f, +3f, +x, -s, 0f, null))
        callback(wheelMesh, getTransform(2).place(+q, -3f, -3f, -x, +s + PIf, 0f, null))
        callback(wheelMesh, getTransform(3).place(-q, -3f, -3f, +x, +s, 0f, null))
    }
}
