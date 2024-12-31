package me.anno.minecraft.rendering.v3

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.minecraft.world.SampleDimensions.sandDim

fun main() {
    val scene = Entity()
    val dimension = sandDim
    scene.add(CachedRendering(dimension, scene))
    testSceneWithUI("CachedRendering", scene)
}