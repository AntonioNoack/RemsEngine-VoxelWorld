package me.anno.remcraft.rendering.cubemaps

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.remcraft.world.SampleDimensions.sandDim

fun main() {
    val scene = Entity()
    val dimension = sandDim
    scene.add(CachedRendering(dimension, scene))
    testSceneWithUI("CachedRendering", scene)
}