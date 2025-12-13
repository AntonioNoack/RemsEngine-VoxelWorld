package me.anno.minecraft.rendering.v2

import me.anno.ecs.Entity
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.createSceneUI
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.entity.ArrowEntity
import me.anno.minecraft.entity.BoarEntity
import me.anno.minecraft.entity.BoarSkeletonEntity
import me.anno.minecraft.entity.MovingBlock
import me.anno.minecraft.entity.MovingEntity
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.entity.physics.CollisionSystem
import me.anno.minecraft.rendering.createLighting
import me.anno.minecraft.ui.*
import me.anno.minecraft.world.SampleDimensions
import me.anno.minecraft.world.SaveLoadSystem
import me.anno.ui.base.groups.PanelList
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3i

val saveSystem = SaveLoadSystem("Minecraft")
val dimension = SampleDimensions.perlin2dDim.apply {
    timeoutMillis = 250
}

val csx = dimension.sizeX
val csy = dimension.sizeY
val csz = dimension.sizeZ

lateinit var chunkLoader: ChunkLoader

private val invalidChunks = HashSet<Vector3i>()
fun invalidateChunk(coords: Vector3i) {
    val needsWorker = synchronized(invalidChunks) {
        invalidChunks.add(coords)
    }
    if (needsWorker) {
        chunkLoader.worker += {
            val changed = synchronized(invalidChunks) {
                invalidChunks.remove(coords)
            }
            assertTrue(changed)
            chunkLoader.generateChunk(coords)
        }
    }
}


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

    // todo implement falling sand
    //  -> if block below is removed,
    //     then spawn a block entity
    //  -> if block entity hits floor (isOnGround)
    //     then set a block

    // todo remove side panels, lock mouse

    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    registerSystem(CollisionSystem)

    val solidRenderer = ChunkRenderer(TextureMaterial.solid)
    val fluidRenderer = ChunkRenderer(TextureMaterial.fluid)
    val detailRenderer = DetailChunkRenderer(TextureMaterial.solid)
    chunkLoader = ChunkLoader(solidRenderer, fluidRenderer, detailRenderer)

    val player = PlayerEntity(isPrimary = true, "Friedolin")
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
    spawnEntity(entities, player)

    spawnEntity(entities, BoarEntity(), Vector3d(-2.0, 77.0, 0.0))
    spawnEntity(entities, BoarSkeletonEntity(), Vector3d(2.0, 77.0, 0.0))
    spawnEntity(
        entities, MovingBlock(ItemSlot(BlockRegistry.Sand, 1, null)),
        Vector3d(0.5, 90.5, 0.5)
    )
    spawnEntity(entities, ArrowEntity(), Vector3d(-2.0, 90.0, 0.0))

    scene.add(solidRenderer)
    scene.add(fluidRenderer)
    scene.add(detailRenderer)
    scene.add(chunkLoader)
    scene.add(createLighting())

    fun init(sceneView: SceneView) {
        val renderer = sceneView.renderView
        val creativeControls = CreativeControls(player, dimension, chunkLoader, renderer)
        val spectatorControls = SpectatorControls(player, dimension, chunkLoader, renderer)
        val survivalControls = SurvivalControls(player, dimension, chunkLoader, renderer)
        val adventureControls = AdventureControls(player, dimension, chunkLoader, renderer)
        val allControls = mapOf(
            ControlMode.CREATIVE to creativeControls,
            ControlMode.SURVIVAL to survivalControls,
            ControlMode.ADVENTURE to adventureControls,
            ControlMode.SPECTATOR to spectatorControls
        )
        for (control in allControls.values) {
            control.controlModes = allControls
        }
        sceneView.editControls = creativeControls
    }

    if (false) {
        testSceneWithUI("RemCraft", scene) { sceneView ->
            init(sceneView)
        }
    } else {
        testUI3("RemCraft") {
            // todo bug: why is nothing being rendered???
            lateinit var sv: SceneView
            val ui = createSceneUI(scene) { init(it); sv = it }
            /*Systems.world = scene
            ECSSceneTabs.open(ECSSceneTab(scene.ref, PlayMode.EDITING), true)
            PrefabInspector.currentInspector = PrefabInspector(scene.ref)
            val sceneView = SceneView(RenderView1(PlayMode.EDITING, scene, style), style)
            init(sceneView)
            sceneView*/

            // todo why is ECSSceneTabs required for things to be rendered???
            ui.forAllPanels { panel ->
                if (panel is PanelList) {
                    panel.children.removeIf { child -> child is PropertyInspector || child is ECSTreeView }
                }
            }
            ui

        }
    }
}

fun spawnEntity(entities: Entity, entity: me.anno.minecraft.entity.Entity) {
    val childEntity = Entity(entities).add(entity)
    if (entity is MovingEntity) {
        childEntity.setPosition(entity.physics.position)
    }
}


fun spawnEntity(entities: Entity, entity: MovingEntity, pos: Vector3d) {
    entity.physics.position.set(pos)
    spawnEntity(entities, entity)
}

