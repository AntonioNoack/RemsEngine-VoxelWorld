package me.anno.minecraft.v2

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.minecraft.world.SampleDimensions

val saveSystem = SaveLoadSystem("Minecraft")
val world = SampleDimensions.perlin3dDim.apply {
    timeoutMillis = 250
}

val csx = world.sizeX
val csy = world.sizeY
val csz = world.sizeZ

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
    val chunkRenderer = ChunkRenderer(TextureMaterial)
    val chunkLoader = ChunkLoader(chunkRenderer)
    scene.add(chunkRenderer)
    scene.add(chunkLoader)
    scene.add(createLighting())
    testSceneWithUI("Minecraft", scene) {
        it.editControls = CreativeControls(scene, chunkLoader, it.renderer)
    }
}