package me.anno.minecraft.rendering

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.maths.Maths.posMod

fun createLighting(): Entity {
    val scene = Entity("Lighting")
    val sun = DirectionalLight()
    sun.shadowMapCascades = 3
    sun.autoUpdate = 0
    val sunEntity = Entity("Sun")
        .setScale(250.0)
    sunEntity.add(object : Component(), OnUpdate {
        // move shadows with player
        var updateCtr = 0
        override fun onUpdate() {
            val rv = RenderView.currentInstance
            if (rv != null && posMod(updateCtr++, 10) == 0) {
                sun.needsUpdate1 = true
                sunEntity.transform.localPosition = rv.orbitCenter
                sunEntity.transform.teleportUpdate()
                sunEntity.validateTransform()
                sun.onUpdate()
            }
        }
    })
    sunEntity.add(sun)
    val sky = Skybox()
    sky.applyOntoSun(sunEntity, sun, 50f)
    scene.add(sky)
    scene.add(sunEntity)
    return scene
}
