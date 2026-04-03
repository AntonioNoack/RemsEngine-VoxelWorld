package me.anno.remcraft.rendering.v2

import me.anno.Engine
import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.createSceneUI
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER64
import me.anno.gpu.WindowManagement
import me.anno.input.MouseLock.unlockMouse
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockUpdateSystem
import me.anno.remcraft.coasters.Minecart
import me.anno.remcraft.entity.*
import me.anno.remcraft.entity.RemcraftEntity.Companion.spawnEntity
import me.anno.remcraft.entity.physics.CollisionSystem
import me.anno.remcraft.rendering.createLighting
import me.anno.remcraft.rendering.v2.ChunkIndex.encodeChunkIndex
import me.anno.remcraft.ui.ItemSlot
import me.anno.remcraft.ui.controls.RemcraftControls
import me.anno.remcraft.world.SampleDimensions
import me.anno.remcraft.world.SaveLoadSystem
import me.anno.ui.base.groups.PanelList
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3i
import org.lwjgl.glfw.GLFW
import speiger.primitivecollections.LongHashSet
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val LOGGER = LogManager.getLogger("RenderingV2")

val saveSystem = SaveLoadSystem("Remcraft")
val dimension = SampleDimensions.perlin2dDim.apply {
    timeoutMillis = 250
}

lateinit var chunkLoader: ChunkLoaderBase<*>
lateinit var entities: Entity
lateinit var player: PlayerEntity

val invalidChunks = LongHashSet()
fun invalidateChunk(coords: Vector3i) {
    val encoded = encodeChunkIndex(coords.x, coords.y, coords.z)
    val needsWorker = synchronized(invalidChunks) {
        invalidChunks.add(encoded)
    }
    if (needsWorker) {
        chunkLoader.worker += {
            synchronized(invalidChunks) {
                invalidChunks.remove(encoded)
            }
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
 * done usable inventory
 * */
fun main() {

    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    registerSystem(CollisionSystem)
    registerSystem(BlockUpdateSystem)

    val solidRenderer = ChunkRenderer(TextureMaterial.solid)
    val fluidRenderer = ChunkRenderer(TextureMaterial.fluid)
    val detailRenderer = DetailChunkRenderer(TextureMaterial.solid)
    chunkLoader = ChunkLoader(solidRenderer, fluidRenderer, detailRenderer)

    player = PlayerEntity(isPrimary = true, "Friedolin")
    player.physics.position.y = 77.0

    // place a few special blocks for testing
    // todo why are some sides invisible???
    for (dz in -3..3) {
        for (dx in -3..3) {
            dimension.setBlockAt(dx, 75, dz, BlockRegistry.Sandstone, null)
        }
    }
    dimension.setBlockAt(1, 76, 0, BlockRegistry.byUUID["remcraft.sandstone.slab[2]"]!!, null)
    dimension.setBlockAt(2, 76, 0, BlockRegistry.byUUID["remcraft.sandstone.slab[3]"]!!, null)
    dimension.setBlockAt(3, 76, 0, BlockRegistry.byUUID["remcraft.sandstone.fence"]!!, null)

    BlockRegistry.SnowLayers.subList(1, 8).forEachIndexed { index, block ->
        dimension.setBlockAt(index, 76, 1, block, null)
    }

    entities = Entity("Entities", scene)
    spawnEntity(player)

    spawnEntity(BoarEntity(), Vector3d(-2.0, 77.0, 0.0))
    spawnEntity(BoarSkeletonEntity(), Vector3d(2.0, 77.0, 0.0))

    // todo spawn a little rail, too
    spawnEntity(Minecart(), Vector3d(0.0, 77.0, -5.0))

    if (false) spawnEntity(
        MovingBlock(ItemSlot(BlockRegistry.Sand, 1, null)),
        Vector3d(0.5, 90.5, 0.5)
    )
    spawnEntity(ArrowEntity(), Vector3d(-2.0, 90.0, 0.0))

    scene.add(solidRenderer)
    scene.add(fluidRenderer)
    scene.add(detailRenderer)
    scene.add(chunkLoader)
    scene.add(createLighting())

    fun init(sceneView: SceneView) {
        val renderer = sceneView.renderView
        sceneView.editControls = RemcraftControls(player, dimension, renderer)
    }

    thread(name = "WatchDog") {
        while (Time.frameIndex == 0) Thread.sleep(10)
        var t0 = Time.nanoTime
        var f0 = Time.frameIndex
        while (!Engine.shutdown) {
            val t1 = Time.nanoTime
            if (t1 > t0 + 5e9) break
            val f1 = Time.frameIndex
            if (f1 > f0) {
                f0 = f1
                t0 = t1
            }
        }
        if (!Engine.shutdown) {
            LOGGER.warn("Got stuck")
            unlockMouse()
            try {
                WindowManagement.updateWindows()
            } catch (e: Exception) {
            }
            try {
                for (window in GFX.windows) {
                    val pointer = window.pointer
                    if (pointer == INVALID_POINTER64) continue
                    GLFW.glfwMakeContextCurrent(pointer)
                    GLFW.glfwSetWindowShouldClose(pointer, true)
                    GLFW.glfwDestroyWindow(pointer)
                    window.pointer = 0L
                }
            } catch (e: Exception) {
            }
            exitProcess(-1)
        }
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
