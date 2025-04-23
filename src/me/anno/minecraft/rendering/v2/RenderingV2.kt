package me.anno.minecraft.rendering.v2

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.entity.MovingEntity
import me.anno.minecraft.entity.Player
import me.anno.minecraft.rendering.createLighting
import me.anno.minecraft.ui.*
import me.anno.minecraft.world.SampleDimensions
import me.anno.minecraft.world.SaveLoadSystem

val saveSystem = SaveLoadSystem("Minecraft")
val dimension = SampleDimensions.perlin2dDim.apply {
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
    val detailRenderer = DetailChunkRenderer(TextureMaterial.solid)
    val chunkLoader = ChunkLoader(solidRenderer, fluidRenderer, detailRenderer)

    val player = Player(isPrimary = true, "Friedolin")
    player.physics.position.y = 77.0

    // place a few special blocks for testing
    // todo why are all these blocks invisible???
    for (dz in -3..3) {
        for (dx in -3..3) {
            dimension.setElementAt(dx, 75, dz, true, BlockRegistry.Sandstone)
        }
    }
    dimension.setElementAt(1, 76, 0, true, BlockRegistry.byUUID["remcraft.sandstone.slab[2]"]!!)
    dimension.setElementAt(2, 76, 0, true, BlockRegistry.byUUID["remcraft.sandstone.slab[3]"]!!)
    dimension.setElementAt(3, 76, 0, true, BlockRegistry.byUUID["remcraft.sandstone.fence"]!!)

    val entities = Entity("Entities", scene)
    spawnPlayer(entities, player)

    scene.add(solidRenderer)
    scene.add(fluidRenderer)
    scene.add(detailRenderer)
    scene.add(chunkLoader)
    scene.add(createLighting())
    testSceneWithUI("Minecraft", scene) {
        val creativeControls = CreativeControls(player, dimension, chunkLoader, it.renderView)
        val spectatorControls = SpectatorControls(player, dimension, chunkLoader, it.renderView)
        val survivalControls = SurvivalControls(player, dimension, chunkLoader, it.renderView)
        val adventureControls = AdventureControls(player, dimension, chunkLoader, it.renderView)
        val allControls = mapOf(
            ControlMode.CREATIVE to creativeControls,
            ControlMode.SURVIVAL to survivalControls,
            ControlMode.ADVENTURE to adventureControls,
            ControlMode.SPECTATOR to spectatorControls
        )
        for (control in allControls.values) {
            control.controlModes = allControls
        }
        it.editControls = creativeControls
    }
}

fun spawnPlayer(scene: Entity, entity: me.anno.minecraft.entity.Entity) {
    val childEntity = Entity(scene).add(entity)
    if (entity is MovingEntity) {
        childEntity.setPosition(entity.physics.position)
    }
}

