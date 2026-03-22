package me.anno.remcraft.rendering.globalillumination.test

import me.anno.ecs.components.light.sky.Skybox
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination
import me.anno.remcraft.rendering.globalillumination.createDebugMesh
import me.anno.remcraft.rendering.globalillumination.createWorld
import me.anno.remcraft.world.Dimension
import org.joml.Vector3f

// implement a prototype on the CPU-side for now
fun main() {

    OfficialExtensions.initForTests()

    lateinit var dimension: Dimension
    val gi by lazy { GlobalIllumination(dimension) }
    createWorld(false) { chunk ->
        dimension = chunk.dimension
        gi.addChunk(chunk)
    }

    println("UniqueFaces: ${gi.faces.size}, Connections: ${gi.connections.totalSize()}")

    val defaultSky = Skybox()
    val sunDir = Vector3f(defaultSky.sunBaseDir)
        .rotate(defaultSky.sunRotation)
        .normalize()
    val sunColors = BlockSide.entries.map { side ->
        val brightness = 5f * sunDir.dot(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
        if (brightness > 0f) {
            Vector3f(1f, 1f, 0.9f).mul(brightness)
        } else null
    }

    val skyColors = BlockSide.entries.map { _ ->
        Vector3f(0.8f, 0.9f, 0.92f).mul(0.5f)
    }

    val light = gi.lightTransport(
        sunDir, sunColors,
        skyColors, 20
    )

    testSceneWithUI("GIMesh", createDebugMesh(gi, light.values))
}
