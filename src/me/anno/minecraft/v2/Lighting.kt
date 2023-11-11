package me.anno.minecraft.v2

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ui.render.RenderView

fun createLighting(): Entity {
    val scene = Entity("Lighting")
    val sun = DirectionalLight()
    sun.shadowMapCascades = 3
    val sunEntity = Entity("Sun")
        .setScale(250.0)
    sunEntity.add(object : Component() {
        // move shadows with player
        // todo only update every so often
        override fun onUpdate(): Int {
            val rv = RenderView.currentInstance
            if (rv != null) {
                sunEntity.transform.localPosition = rv.orbitCenter
                sunEntity.validateTransform()
            }
            return 1
        }
    })
    sunEntity.add(sun)
    val sky = Skybox()
    sky.applyOntoSun(sunEntity, sun, 50f)
    scene.add(sky)
    scene.add(sunEntity)
    return scene
}
