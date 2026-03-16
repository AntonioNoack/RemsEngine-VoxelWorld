package me.anno.remcraft.coasters

import me.anno.ecs.Entity
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.remcraft.entity.PlayerEntity
import me.anno.remcraft.entity.physics.CollisionSystem
import me.anno.remcraft.rendering.v2.player
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {

    player = PlayerEntity(isPrimary = true, "Luise")

    val scene = Entity("Scene")
    registerSystem(CollisionSystem)

    val a = CoasterRailBase(Vector3d(-10.0, 0.0, -16.0), Quaternionf().rotationY(90f.toRadians()))
    val b = CoasterRailBase(Vector3d(0.0, -1.0, -10.0), Quaternionf())
    val c = CoasterRailBase(Vector3d(0.0, +1.0, +10.0), Quaternionf())

    fun addStrip(strip: CoasterRail) {
        Entity("Strip", scene)
            .setPosition(strip.center)
            .add(strip)
    }

    addStrip(CoasterRail(a, b))
    addStrip(CoasterRail(b, c))

    Entity("Minecart", scene)
        .add(Minecart())

    testSceneWithUI("Coaster Physics", scene)
}