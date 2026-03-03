package me.anno.minecraft.ui.controls

import me.anno.Time
import me.anno.engine.Events.addEvent
import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.framebuffer.Screenshots
import me.anno.input.Input
import me.anno.input.Key
import me.anno.jvm.OpenFileExternallyImpl.openInExplorer
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.posMod
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.rendering.v2.player
import me.anno.minecraft.ui.Inventory
import me.anno.minecraft.ui.components.HeartPanel
import me.anno.minecraft.ui.components.HungerPanel
import me.anno.minecraft.ui.components.ItemPanel
import me.anno.minecraft.ui.components.ItemPanel.Companion.drawDraggedItem
import me.anno.minecraft.ui.components.XpBarPanel
import me.anno.minecraft.world.Dimension
import me.anno.ui.Panel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toFloat
import me.anno.utils.types.Floats.toDegrees
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBi
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

abstract class MinecraftControls(
    val sceneView: SceneView,
    val player: PlayerEntity,
    val dimension: Dimension,
    renderer: RenderView
) : ControlScheme(renderer) {

    companion object {

        private val LOGGER = LogManager.getLogger(MinecraftControls::class)

        var inHandSlot = 0

        val inHand get() = inventory.slots[inHandSlot]
        val inHandItem get() = inHand.type
        val inHandMetadata get() = inHand.metadata

        val inventorySizeX = 9
        val inventory get() = player.inventory

        val clickDistanceDelta = 0.001

        var lookedAtInventory: Inventory? = null
    }

    val modeButton = TextButton(NameDesc("Next Mode"), style)

    // todo open creative inventory
    // todo and open your own inventory for survival mode

    val inventoryUI = PanelListY(style)
    val escapeUI = PanelListY(style)
    val inventoryBar = PanelListY(style)
    val inventoryBar1 = PanelListX(style)
    val chatMessagesPanel = PanelListY(style)

    init {
        for ((i, type) in listOf(
            BlockRegistry.byUUID["remcraft.sandstone.slab[2]"]!!,
            BlockRegistry.byUUID["remcraft.sandstone.fence"]!!,
            BlockRegistry.Dirt, BlockRegistry.Grass, BlockRegistry.Water, BlockRegistry.Lava,
            BlockRegistry.Sand, BlockRegistry.Sandstone, BlockRegistry.Cactus, BlockRegistry.Stone,
            BlockRegistry.Gravel,
            BlockRegistry.Chest, BlockRegistry.Furnace, BlockRegistry.Hopper,
        ).withIndex()) {
            val slot = inventory.slots.getOrNull(i) ?: break
            slot.set(type, 10, null)
        }

        // inventoryBar.add(ItemPanel(offHand.slots[0], Int.MAX_VALUE))

        inventoryBar1.add(SpacerPanel(10, 0, style))
        for (i in 0 until inventorySizeX) {
            inventoryBar1.add(ItemPanel(inventory.slots[i], i))
        }
        val hungerHealth = PanelListX(style).apply { makeBackgroundTransparent() }
        hungerHealth.add(HeartPanel(style))
        hungerHealth.add(HungerPanel(style))
        inventoryBar.add(hungerHealth)
        inventoryBar.add(XpBarPanel(style))
        inventoryBar.add(inventoryBar1)
        inventoryBar.alignmentX = AxisAlignment.CENTER
        inventoryBar.alignmentY = AxisAlignment.MAX
        add(inventoryBar)

        for (i in 0 until 6) {
            val list = PanelListX(style)
            for (j in 0 until inventorySizeX) {
                val index = (i + 1) * inventorySizeX + j
                list.add(ItemPanel(inventory.slots[index], index))
            }
            inventoryUI.add(list)
        }

        inventoryUI.alignmentX = AxisAlignment.CENTER
        inventoryUI.alignmentY = AxisAlignment.CENTER
        inventoryUI.isVisible = false
        add(inventoryUI)

        modeButton.addLeftClickListener { changeToNextGameMode() }
        modeButton.alignmentX = AxisAlignment.MAX
        modeButton.alignmentY = AxisAlignment.MIN
        add(modeButton)

        escapeUI.alignmentX = AxisAlignment.CENTER
        escapeUI.alignmentY = AxisAlignment.CENTER
        escapeUI.isVisible = false
        add(escapeUI)

        chatMessagesPanel.alignmentX = AxisAlignment.MIN
        chatMessagesPanel.alignmentY = AxisAlignment.MAX
        chatMessagesPanel.background.color =
            chatMessagesPanel.background.color.withAlpha(127)
        add(chatMessagesPanel)

        // set initial rotation
        rotatePlayer(0f, 0f)

        zoom(2f / renderView.radius)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        drawCrosshair()
        drawDraggedItem(window, inventoryBar1.height)
    }

    fun drawCrosshair() {
        // draw crosshair; todo xor colors
        DrawRectangles.drawRect(x + width / 2, y + height / 2, 2, 2, Color.white)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (lookedAtInventory != null) {
            // todo if is in menu (inventory), handle super
            unlockMouse()
            super.onMouseMoved(x, y, dx, dy)
        } else {
            val sign = if (player.firstPersonMode) -1f else +1f
            rotatePlayer(dx * sign, dy * sign)
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        showHoveredBlock()
    }

    fun showHoveredBlock() {
        val query = clickCast() ?: return
        val coords = getCoords(query, +clickDistanceDelta)
        val block = getBlock(coords) ?: return
        if (block != BlockRegistry.Air) {
            val bounds = AABBd()
            block.getBounds(coords.x, coords.y, coords.z, bounds)
                .addMargin(0.001)
            DebugShapes.showDebugAABB(
                DebugAABB(bounds, -1, 0f)
            )
        }
    }

    fun openInventory(inventory: Inventory, ui: Panel) {

    }

    fun rotatePlayer(dx: Float, dy: Float) {
        val entity = player.entity!!
        val transform = entity.transform
        val speed = 5f / (width + height)
        player.smoothAngle0 -= dx * speed
        transform.localRotation = transform.localRotation
            .rotationY(player.bodyRotationY + dx * speed)
        player.headRotation
            .rotationX(player.headRotationX + dy * speed)
        rotationTargetDegrees.set(
            player.headRotationX.toDouble().toDegrees(),
            player.bodyRotationY.toDouble().toDegrees(),
            0.0 // todo set this to wobble after explosions
        )
    }

    override fun updateEditorCameraTransform() {
        val renderView = renderView
        renderView.orbitCenter.set(player.physics.position)
        val radius = if (player.firstPersonMode) 0f else 5f

        val camera = renderView.editorCamera
        val cameraNode = renderView.editorCameraNode

        camera.far = 1e6f
        camera.near = 0.1f

        val tmp3d = JomlPools.vec3d.borrow()
        cameraNode.transform.localPosition =
            renderView.orbitRotation.transform(tmp3d.set(0f, 0f, radius))
                .add(renderView.orbitCenter).apply {
                    // always have eyes before body, even if looking up or down
                    val radiusX = 0.4f
                    val rotY = player.bodyRotationY
                    x -= sin(rotY) * radiusX
                    z -= cos(rotY) * radiusX
                    y += if (player.isSneaking) 0.4f else 0.6f // adding player eye height...
                }

        cameraNode.transform.localRotation = renderView.orbitRotation
        cameraNode.transform.teleportUpdate()
        cameraNode.validateTransform()
    }

    abstract val canFly: Boolean

    var isFlying = false
    var isRunning = false

    fun applyPlayerMovement() {
        if (this is SpectatorControls) isFlying = true
        val dt = Time.deltaTime.toFloat()
        val dy = when {
            canFly && isFlying -> (Input.isKeyDown(Key.KEY_SPACE).toFloat() - Input.isControlDown.toFloat()) * 100f
            else -> 0f
        }

        player.gravityFactor = if (isFlying) 0f else 1f
        player.isSneaking = Input.isShiftDown

        if (Input.isShiftDown) isRunning = true
        val moveSpeed = when {
            isFlying -> 100f
            !player.physics.isOnGround -> 1f
            isRunning -> 20f
            else -> 10f
        }

        val moveIntend = Vector3f()
        moveIntend.set(
            checkKeys(Key.KEY_D, Key.KEY_ARROW_RIGHT) - checkKeys(Key.KEY_A, Key.KEY_ARROW_LEFT), dy,
            checkKeys(Key.KEY_S, Key.KEY_ARROW_DOWN) - checkKeys(Key.KEY_W, Key.KEY_ARROW_UP)
        ).rotateY(player.bodyRotationY).mul(moveSpeed, 1f, moveSpeed)
        if (dy == 0f && player.physics.isOnGround && Input.wasKeyPressed(Key.KEY_SPACE)) player.jump()

        // if player is flying, add artificial friction
        if (isFlying) {
            moveIntend.fma(-0.1f / dt, player.physics.actualVelocity)
        }

        player.physics.acceleration.add(moveIntend)

        // shift: sprint
        // control: duck
    }

    fun checkKeys(key1: Key, key2: Key): Float {
        return (Input.isKeyDown(key1) || Input.isKeyDown(key2)).toFloat()
    }

    lateinit var gameModeUIs: Map<GameMode, MinecraftControls>

    fun changeToNextGameMode() {
        val thisEnum = player.gameMode
        val nextEnum = GameMode.entries[posMod(thisEnum.ordinal + 1, GameMode.entries.size)]
        val nextControls = gameModeUIs[nextEnum]!!

        println("Changing gameMode from ${javaClass.simpleName} to ${nextControls.javaClass.simpleName}")

        val sv = sceneView
        renderView.controlScheme = nextControls
        sv.editControls = nextControls
        sv.playControls = nextControls
        player.gameMode = nextEnum
    }

    var lastSpaceTime = 0L
    var lastWTime = 0L

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_F4 -> changeToNextGameMode()
            Key.KEY_SPACE -> {
                // double space to fly
                val time = Time.nanoTime
                if (abs(time - lastSpaceTime) < 300 * MILLIS_TO_NANOS) {
                    isFlying = canFly && !isFlying
                }
                lastSpaceTime = time
            }
            Key.KEY_W -> {
                // double-w to running
                val time = Time.nanoTime
                if (abs(time - lastWTime) < 300 * MILLIS_TO_NANOS) {
                    isRunning = true
                }
                lastWTime = time
            }
            Key.KEY_E -> {
                inventoryUI.isVisible = !inventoryUI.isVisible
                if (inventoryUI.isVisible) unlockMouse()
            }
            Key.KEY_1, Key.KEY_KP_1 -> inHandSlot = 0
            Key.KEY_2, Key.KEY_KP_2 -> inHandSlot = 1
            Key.KEY_3, Key.KEY_KP_3 -> inHandSlot = 2
            Key.KEY_4, Key.KEY_KP_4 -> inHandSlot = 3
            Key.KEY_5, Key.KEY_KP_5 -> inHandSlot = 4
            Key.KEY_6, Key.KEY_KP_6 -> inHandSlot = 5
            Key.KEY_7, Key.KEY_KP_7 -> inHandSlot = 6
            Key.KEY_8, Key.KEY_KP_8 -> inHandSlot = 7
            Key.KEY_9, Key.KEY_KP_9 -> inHandSlot = 8
            else -> super.onKeyTyped(x, y, key)
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        inventoryUI.isVisible = false
        escapeUI.isVisible = false
        lockMouse()
    }

    override fun onEscapeKey(x: Float, y: Float) {
        when {
            escapeUI.isVisible -> escapeUI.isVisible = false
            inventoryUI.isVisible -> inventoryUI.isVisible = false
            else -> unlockMouse()
        }
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_LEFT_SHIFT, Key.KEY_RIGHT_SHIFT -> isRunning = true
            Key.KEY_F1 -> inventoryBar.isVisible = !inventoryBar.isVisible
            Key.KEY_F2 -> takeAndStoreScreenshot()
            Key.KEY_F4 -> changeToNextGameMode()
            Key.KEY_F5 -> player.firstPersonMode = !player.firstPersonMode
            Key.BUTTON_LEFT, Key.BUTTON_RIGHT -> {
                if (!inventoryUI.isVisible && !escapeUI.isVisible) {
                    lockMouse()
                }
            }
            else -> super.onKeyDown(x, y, key)
        }
    }

    fun takeAndStoreScreenshot() {
        val window = window ?: return
        val dstFile = Screenshots.getNextScreenshotFile() ?: return
        addGPUTask("Screenshot", 1) {
            val image = window.buffer.createImage(flipY = false, withAlpha = false)
            if (image != null) {
                image.cropped(x, y, width, height).write(dstFile)
                image.destroy()
                addSystemChatMessage("Saved screenshot as '$dstFile'") {
                    unlockMouse()
                    openInExplorer(dstFile)
                }
            }
        }
    }

    fun addSystemChatMessage(message: String, onClick: (() -> Unit)? = null) {
        LOGGER.info(message)
        // todo underline the text, if we can click it
        val panel = TextPanel(message, style)
        panel.makeBackgroundTransparent()
        if (onClick != null) {
            panel.addLeftClickListener { onClick() }
        }
        chatMessagesPanel.add(panel)
        // hide message after time X
        addEvent(3000) { panel.removeFromParent() }
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_LEFT_SHIFT, Key.KEY_RIGHT_SHIFT -> isRunning = false
            else -> super.onKeyDown(x, y, key)
        }
    }

    fun getCoords(query: RayQuery, delta: Double): Vector3i {
        val pos = query.result.positionWS
        val dir = query.direction
        dir.mulAdd(delta, pos, pos)
        return Vector3i(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
    }

    fun getBlock(coords: Vector3i): BlockType? {
        return dimension.getBlockAt(coords.x, coords.y, coords.z)
    }

    fun getBlockMetadata(coords: Vector3i): Metadata? {
        return dimension.getMetadataAt(coords.x, coords.y, coords.z)
    }

    fun setBlock(coords: Vector3i, type: BlockType, metadata: Metadata?): Boolean {
        val chunk = dimension.getChunkAt(coords.x, coords.y, coords.z) ?: return false
        chunk.setBlock(coords.x, coords.y, coords.z, type, metadata)
        // chunk.afterBlockChange(coords.x, coords.y, coords.z)
        addEvent(50) { // todo this should not be needed!!!
            chunk.afterBlockChange(coords.x, coords.y, coords.z)
        }
        return true
    }

    abstract fun getReachDistance(): Double

    fun clickCast(): RayQuery? {
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = RayQuery(
            renderView.cameraPosition,
            renderView.mouseDirection,
            getReachDistance()
        )

        val queryBounds = AABBi()
        // todo these bounds aren't working correctly... why???
        // queryBounds.union(query.start.x.toInt(), query.start.y.toInt(), query.start.z.toInt())
        // queryBounds.addMargin(ceil(query.result.distance + 1.0).toInt())
        queryBounds.all()

        val hitSomething =
            BlockTracing.blockTrace(query, (query.result.distance * 3).toInt(), queryBounds) { xi, yi, zi ->
                val chunk = dimension.getChunk(
                    xi shr dimension.bitsX,
                    yi shr dimension.bitsY,
                    zi shr dimension.bitsZ,
                    false
                )?.value
                val block = chunk?.getBlock(xi, yi, zi) ?: BlockRegistry.Air
                if (block.isSolid) BlockTracing.SOLID_BLOCK
                else BlockTracing.AIR_BLOCK
            }

        return if (hitSomething) query else null
    }

}