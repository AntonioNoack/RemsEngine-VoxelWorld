package me.anno.remcraft.coasters

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {
    val st0 = CoasterRailBase(Vector3d(0.0, 0.0, 0.0), Quaternionf())
    val st1 = CoasterRailBase(Vector3d(3.0, 3.0, 10.0), Quaternionf().rotateZ(PIf * 0.5f))
    val strip = CoasterRail(st0, st1)
    testSceneWithUI("CoasterStrip", strip)
}