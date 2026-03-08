package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderState
import me.anno.minecraft.entity.PlayerEntity

class XpModel(private val mesh: Mesh) : Model<PlayerEntity>() {
    override fun fill(transform: Transform, callback: (Mesh, Transform) -> Unit) {
        // if (pipeline == LightComponent.pipeline) return // no shadow

        val pos = physics.position
        val cam = RenderState.cameraPosition

        // todo check whether lookAt-player works
        transform.localRotation = transform.localRotation
            .rotationTo(
                0f, 0f, 1f,
                (pos.x - cam.x).toFloat(),
                (pos.y - cam.y).toFloat(),
                (pos.z - cam.z).toFloat(),
            )

        callback(mesh, transform)
    }
}