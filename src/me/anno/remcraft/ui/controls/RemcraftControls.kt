package me.anno.remcraft.ui.controls

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.Events.addEvent
import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugRay
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.RayQuery
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.framebuffer.Screenshots
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.input.Key
import me.anno.jvm.OpenFileExternallyImpl.openInExplorer
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.posMod
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.block.BlockType.Companion.dropItem
import me.anno.remcraft.block.BlockType.Companion.getDropPosition
import me.anno.remcraft.block.Metadata
import me.anno.remcraft.block.impl.BlockWithInventory
import me.anno.remcraft.entity.Animal
import me.anno.remcraft.entity.PlayerEntity
import me.anno.remcraft.entity.RightClickAnimal
import me.anno.remcraft.item.Mining.getMiningDuration
import me.anno.remcraft.item.RightClickBlock
import me.anno.remcraft.item.RightClickItem
import me.anno.remcraft.rendering.v2.player
import me.anno.remcraft.ui.BreakModels
import me.anno.remcraft.ui.ItemSlot
import me.anno.remcraft.ui.components.HeartPanel
import me.anno.remcraft.ui.components.HungerPanel
import me.anno.remcraft.ui.components.ItemPanel
import me.anno.remcraft.ui.components.ItemPanel.Companion.drawDraggedItem
import me.anno.remcraft.ui.components.XpBarPanel
import me.anno.remcraft.ui.controls.Raycast.clickCast
import me.anno.remcraft.world.Dimension
import me.anno.ui.Panel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Color
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toFloat
import me.anno.utils.types.Floats.toDegrees
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.*

class RemcraftControls(
    val player: PlayerEntity,
    val dimension: Dimension,
    renderer: RenderView
) : ControlScheme(renderer) {

    companion object {

        private val LOGGER = LogManager.getLogger(RemcraftControls::class)

        var inHandSlot: Int
            get() = player.inHandSlot
            set(value) {
                player.inHandSlot = value
            }

        val inHand get() = inventory[inHandSlot]
        val inHandItem get() = inHand.type
        val inHandMetadata get() = inHand.metadata

        val inventorySizeX = 9
        val inventory get() = player.inventory

        val clickDistanceDelta = 0.001
    }

    val modeButton = TextButton(NameDesc("Next Mode"), style)

    // todo open creative inventory
    // todo and open your own inventory for survival mode

    val inventoryUI = PanelListY(style)
    val escapeUI = PanelListY(style)
    val inventoryBar = PanelListY(style)
    val inventoryBar1 = PanelListX(style)
    val chatMessagesPanel = PanelListY(style)

    var chestUI: Panel? = null

    init {
        for ((i, type) in listOf(
            BlockRegistry.byUUID["remcraft.sandstone.slab[2]"]!!,
            BlockRegistry.byUUID["remcraft.sandstone.fence"]!!,
            BlockRegistry.Dirt, BlockRegistry.Grass, BlockRegistry.Water, BlockRegistry.Lava,
            BlockRegistry.Sand, BlockRegistry.Sandstone, BlockRegistry.Cactus, BlockRegistry.Stone,
            BlockRegistry.Gravel, BlockRegistry.TallGrass,
            BlockRegistry.Chest, BlockRegistry.Furnace, BlockRegistry.Hopper,
        ).withIndex()) {
            val slot = inventory.slots.getOrNull(i) ?: break
            slot.set(type, 10, null)
        }

        // inventoryBar.add(ItemPanel(offHand.slots[0], Int.MAX_VALUE))

        inventoryBar1.add(SpacerPanel(10, 0, style))
        for (i in 0 until inventorySizeX) {
            inventoryBar1.add(ItemPanel(inventory[i], i))
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
                list.add(ItemPanel(inventory[index], index))
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
        if (inventoryUI.isVisible) {
            drawDraggedItem(window, inventoryBar1.height)
        }
    }

    fun drawCrosshair() {
        // draw crosshair; todo xor colors
        DrawRectangles.drawRect(x + width / 2, y + height / 2, 2, 2, Color.white)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (escapeUI.isVisible || inventoryUI.isVisible) {
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
        applyPlayerMovement()
        showHoveredBlock()
    }

    var hoverResult: RayQuery? = null

    fun showHoveredBlock() {
        val q = prepareQuery()
        hoverResult = clickCast(q, player, dimension)

        if (false && hoverResult == null) {
            DebugShapes.showDebugRay(
                DebugRay(
                    Vector3d(q.start).fma(3f, q.direction),
                    Vector3d(q.direction), -1, 5f
                )
            )
        }

        val query = hoverResult ?: return

        if (false) DebugShapes.showDebugArrow(
            DebugLine(
                Vector3d(q.start),
                Vector3d(q.result.positionWS), -1, 5f
            )
        )

        if (hoversAnimal()) {
            // todo draw animal AABB(s)
            return
        }

        val coords = getCoords(query, +clickDistanceDelta)
        val block = getBlock(coords) ?: return

        if (block != BlockRegistry.Air) {
            val bounds = AABBd()
            block.getBounds(coords.x, coords.y, coords.z, bounds).addMargin(0.001)
            DebugShapes.showDebugAABB(DebugAABB(bounds, -1, 0f))

            if (player.gameMode.canSlowlyMine() &&
                isMining && durationIsMining >= getMiningDuration(block, inHand, 0)
            ) {
                resetMiningDuration()
                // todo if tool is not matching, drop nothing, just set to air
                val metadata = getBlockMetadata(coords)
                if (block is BlockWithInventory) {
                    dropInventory(coords, block)
                }
                block.dropAsItem(coords.x, coords.y, coords.z, metadata, inHand)
            }
        }
    }

    fun resetMiningDuration() {
        Input.keysDown[Key.BUTTON_LEFT] = Time.nanoTime
    }

    fun openInventory(ui: Panel) {
        chestUI?.removeFromParent()
        chestUI = ui
        inventoryUI.add(ui)
        inventoryUI.isVisible = true
        unlockMouse()
    }

    fun closeInventory() {
        chestUI?.removeFromParent()
        chestUI = null
        inventoryUI.isVisible = false
        lockMouse()
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

    val canFly: Boolean get() = player.gameMode.canFly()

    var isFlying = false
    var isRunning = false

    fun applyPlayerMovement() {
        if (player.gameMode.alwaysFlying()) {
            isFlying = true
        }

        val dt = Time.deltaTime.toFloat()
        val dy = when {
            canFly && isFlying -> (Input.isKeyDown(Key.KEY_SPACE).toFloat() - Input.isControlDown.toFloat()) * 100f
            else -> 0f
        }

        player.gravityFactor = if (isFlying) 0f else 1f
        player.isSneaking = Input.isShiftDown
        player.usingLeftHand = Input.isRightDown
        player.usingRightHand = Input.isLeftDown

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

    fun changeToNextGameMode() {
        val thisEnum = player.gameMode
        val nextEnum = GameMode.entries[posMod(thisEnum.ordinal + 1, GameMode.entries.size)]
        println("Changed gameMode from $thisEnum to $nextEnum")
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

    fun dropInventory(coords: Vector3i, dropped: BlockWithInventory) {
        // if it has an inventory, drop all contents
        val metadata = dimension.getOrCreateMetadataAt(coords.x, coords.y, coords.z)
        val inventory = dropped.getOrCreateInventory(metadata)
        val dropPosition = getDropPosition(coords.x, coords.y, coords.z)
        for (slot in inventory.slots) {
            if (slot.isNotEmpty()) dropItem(dropPosition, slot)
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (escapeUI.isVisible) {
            escapeUI.isVisible = false
            if (!inventoryUI.isVisible) lockMouse()
            return
        } else if (inventoryUI.isVisible) {
            closeInventory()
            return
        }

        // find, which block was clicked
        // expensive way, using raycasting:
        val query = hoverResult
        when (button) {
            Key.BUTTON_LEFT -> {
                // remove block
                val animal = query?.result?.component as? Animal
                if (animal != null) {
                    attackAnimal(animal, inHand)
                } else if (query != null && player.gameMode.canInstantlyMine()) {
                    // dropping not necessary
                    val coords = getCoords(query, +clickDistanceDelta)
                    val dropped = getBlock(coords)
                    if (dropped is BlockWithInventory) {
                        dropInventory(coords, dropped)
                    }

                    setBlock(coords, BlockRegistry.Air, null)
                }
            }
            Key.BUTTON_RIGHT -> {
                // add block
                val inHand = inHand
                val inHandType = inHand.type
                if (query != null) {
                    val animal = query.result.component as? Animal
                    if (animal is RightClickAnimal) {
                        animal.onRightClick(this, inHand)
                    } else {
                        val activeCoords = getCoords(query, +clickDistanceDelta)
                        val activeBlock = getBlock(activeCoords)
                        if (activeBlock is RightClickBlock && !Input.isShiftDown) {
                            activeBlock.onRightClickBlock(this, activeCoords)
                        } else {
                            val placeCoords = getCoords(query, -clickDistanceDelta)
                            if (player.gameMode.canPlaceBlocks() &&
                                inHandType is BlockType &&
                                getBlock(placeCoords) == BlockRegistry.Air &&
                                inHandType != BlockRegistry.Air
                            ) {
                                if (player.gameMode.finiteInventory()) inHand.removeOne()
                                setBlock(placeCoords, inHandType, inHandMetadata)
                            } else if (inHandType is RightClickItem) {
                                inHandType.onRightClickItem(player, inHand, placeCoords)
                            }
                        }
                    }
                } else if (inHandType is RightClickItem) {
                    inHandType.onRightClickItem(player, inHand, null)
                }
            }
            Key.BUTTON_MIDDLE -> {
                // get block
                query ?: return
                if (!player.gameMode.canPickBlocks()) return

                val animal = query.result.component as? Animal
                if (animal != null) {
                    // todo pick spawn egg for animal
                } else {
                    val coords = getCoords(query, +clickDistanceDelta)
                    val slot = inventory[inHandSlot]
                    val found = getBlock(coords) ?: BlockRegistry.Air
                    if (found != BlockRegistry.Air) {
                        slot.set(found, 1, getBlockMetadata(coords))
                    }
                }
            }
            else -> {}
        }
    }

    fun attackAnimal(animal: Animal, inHand: ItemSlot) {
        // todo calculate damage
        // todo if animal is passive, run away
        // todo if animal is passive-aggressive, make aggressive & target player
        // todo if animal is aggressive, target player
        // todo play hurt sound
        // todo major damage was blocked, play blocking sound
        animal.damage(5f)

        // todo if animal is hovered, display its health
        println("new health: ${animal.health}")
    }

    override fun onEscapeKey(x: Float, y: Float) {
        when {
            escapeUI.isVisible -> escapeUI.isVisible = false
            inventoryUI.isVisible -> closeInventory()
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

    private val breakTransform = Transform()
    private val renderer = MeshComponent()

    override fun fill(pipeline: Pipeline) {
        super.fill(pipeline)
        drawBlockBreakProgress(pipeline)
    }

    val isMining get() = Input.isLeftDown
    val durationIsMining get() = if (isMining) Input.getDownTimeNanos(Key.BUTTON_LEFT) * 1e-9f else 0f

    fun drawBlockBreakProgress(pipeline: Pipeline) {
        if (!isMining || !player.gameMode.canSlowlyMine() || hoversAnimal()) return

        val query = hoverResult ?: return
        val coords = getCoords(query, +clickDistanceDelta)
        val blockType = dimension.getBlockAt(coords.x, coords.y, coords.z) ?: BlockRegistry.Stone
        val miningDuration = getMiningDuration(blockType, inHand, 0)
        val progress = clamp(durationIsMining / miningDuration)

        breakTransform.localPosition = breakTransform.localPosition.set(coords).add(0.5)
        breakTransform.localScale = breakTransform.localScale.set(0.51f)

        // todo preload all textures...
        //  or preload the next one...

        val stages = BreakModels.materials
        val numLevels = stages.size
        val material = stages[min((progress * numLevels).toInt(), numLevels - 1)]
        // tint material by underlying texture...
        blockType.color.toVecRGBA(material.diffuseBase).apply { w = 1f }
        pipeline.addMesh(BreakModels.cube, renderer, listOf(material.ref), breakTransform)
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
        chunk.setBlock(coords, type, metadata)
        chunk.afterBlockChangeI(coords.x, coords.y, coords.z)
        return true
    }

    fun hoversAnimal() = hoverResult?.result?.component is Animal

    fun prepareQuery(): RayQuery {
        return RayQuery(
            renderView.cameraPosition,
            renderView.mouseDirection,
            player.gameMode.getReachDistance()
        )
    }

}