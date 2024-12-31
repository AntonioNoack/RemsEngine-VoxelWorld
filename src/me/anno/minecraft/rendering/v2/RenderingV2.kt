package me.anno.minecraft.rendering.v2

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.minecraft.rendering.createLighting
import me.anno.minecraft.ui.CreativeControls
import me.anno.minecraft.world.SampleDimensions
import me.anno.minecraft.world.SaveLoadSystem

val saveSystem = SaveLoadSystem("Minecraft")
val dimension = SampleDimensions.perlin3dDim.apply {
    timeoutMillis = 250
}

val csx = dimension.sizeX
val csy = dimension.sizeY
val csz = dimension.sizeZ

/**
 * load/unload a big voxel world without much stutter;
 * glBufferData() unfortunately lags every once in a while, but that should be fine,
 * because it's a few times and then newer again
 *
 * (Minecraft like)
 *
 * done dynamic chunk unloading
 * done load/save system
 * done block placing
 *
 * todo first person player controller with simple physics
 * todo inventory system
 * */
fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    val solidRenderer = ChunkRenderer(TextureMaterial.solid)
    val fluidRenderer = ChunkRenderer(TextureMaterial.fluid)
    val chunkLoader = ChunkLoader(solidRenderer, fluidRenderer)
    scene.add(solidRenderer)
    scene.add(fluidRenderer)
    scene.add(chunkLoader)
    scene.add(createLighting())
    testSceneWithUI("Minecraft", scene) {
        it.editControls = CreativeControls(dimension, chunkLoader, it.renderView)
    }
}
