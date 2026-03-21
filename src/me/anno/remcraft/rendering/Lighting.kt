package me.anno.remcraft.rendering

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.maths.Maths.sq
import me.anno.remcraft.rendering.v2.player

fun createLighting(): Entity {
    val scene = Entity("Lighting")
    val sun = DirectionalLight()
    sun.shadowMapCascades = 3
    sun.autoUpdate = 0
    val sunEntity = Entity("Sun")
        .setScale(250f)
    sunEntity.add(object : Component(), OnUpdate {
        // move shadows with player
        override fun onUpdate() {
            val rv = RenderView.currentInstance
            if (rv != null) {
                sun.needsUpdate1 = true
                val player = player
                if (sunEntity.transform.localPosition.distanceSquared(player.position) > sq(16.0)) {
                    sunEntity.transform.localPosition = player.position
                    sunEntity.transform.teleportUpdate()
                }
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
