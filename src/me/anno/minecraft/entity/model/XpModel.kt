package me.anno.minecraft.entity.model

import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.pipeline.Pipeline
import me.anno.minecraft.entity.PlayerEntity

class XpModel(private val mesh: Mesh) : Model<PlayerEntity>() {
    override fun fill(pipeline: Pipeline, transform: Transform) {
        if (pipeline == LightComponent.pipeline) return // no shadow

        val pos = physics.position
        val cam = RenderState.cameraPosition

        // todo check whether lookAt-player works
        transform.localRotation = transform.localRotation
            .identity()
            .lookAlong(
                (pos.x - cam.x).toFloat(),
                (pos.y - cam.y).toFloat(),
                (pos.z - cam.z).toFloat(),
                0f, 1f, 0f
            )

        pipeline.addMesh(mesh, self, transform)
    }
}