package me.anno.minecraft.ui

import me.anno.Time
import me.anno.engine.debug.DebugAABB
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.posMod
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.world.Dimension
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.types.Booleans.toFloat
import me.anno.utils.types.Floats.toDegrees
import org.joml.AABBd
import org.joml.AABBi
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.abs
import kotlin.math.floor

abstract class MinecraftControls(
    val player: PlayerEntity,
    val dimension: Dimension,
    renderer: RenderView
) : ControlScheme(renderer) {

    companion object {

        var inHandSlot = 0

        val inHand get() = inventory.slots[inHandSlot]
        val inHandItem get() = inHand.type
        val inHandMetadata get() = inHand.metadata

        val inventorySizeX = 9
        val inventory = Inventory(inventorySizeX * (6 + 1))
        val offHand = Inventory(1)

        val clickDistanceDelta = 0.001

        var lookedAtInventory: Inventory? = null
    }

    val modeButton = TextButton(NameDesc("Next Mode"), style)

    // todo open creative inventory
    // todo and open your own inventory for survival mode

    val inventoryUI = PanelListY(style)

    init {
        for ((i, type) in listOf(
            BlockRegistry.byUUID["remcraft.sandstone.slab[2]"]!!,
            BlockRegistry.byUUID["remcraft.sandstone.fence"]!!,
            BlockRegistry.Dirt, BlockRegistry.Grass, BlockRegistry.Water, BlockRegistry.Lava,
            BlockRegistry.Sand, BlockRegistry.Sandstone, BlockRegistry.Cactus, BlockRegistry.Stone,
            BlockRegistry.Gravel
        ).withIndex()) {
            val slot = inventory.slots.getOrNull(i) ?: break
            slot.set(type, 1, null)
        }
        val inventoryBar = PanelListX(style)
        inventoryBar.add(ItemPanel(offHand.slots[0], Int.MAX_VALUE))
        inventoryBar.add(SpacerPanel(10, 0, style))
        for (i in 0 until inventorySizeX) {
            inventoryBar.add(ItemPanel(inventory.slots[i], i))
        }
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

        modeButton.addLeftClickListener { nextMode() }
        modeButton.alignmentX = AxisAlignment.MAX
        modeButton.alignmentY = AxisAlignment.MIN
        add(modeButton)

        // set initial rotation
        rotatePlayer(0f, 0f)

        zoom(2f / renderView.radius)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // todo if is in menu (inventory), handle super
        if (lookedAtInventory != null) {
            super.onMouseMoved(x, y, dx, dy)
        } else if (Input.isRightDown) { // todo always do it...
            rotatePlayer(dx, dy)
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

    fun rotatePlayer(dx: Float, dy: Float) {
        val entity = player.entity!!
        val transform = entity.transform
        val speed = 5f / (width + height)
        transform.localRotation = transform.localRotation
            .rotationY(player.bodyRotationY + dx * speed)
        // todo configure/adjust head rotation
        player.headRotation
            .rotationX(player.headRotationX + dy * speed)
        rotationTargetDegrees.set(
            player.headRotationX.toDouble().toDegrees(),
            player.bodyRotationY.toDouble().toDegrees(),
            0.0 // todo set this to wobble after explosions
        )
    }

    fun updatePlayerCamera() {
        renderView.orbitCenter.set(player.physics.position)
        renderView.updateEditorCameraTransform()
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
        player.spectatorMode = this is SpectatorControls

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

    lateinit var controlModes: Map<ControlMode, MinecraftControls>

    fun nextMode() {
        val thisEnum = controlModes.entries.first { it.value == this }.key
        val nextEnum = ControlMode.entries[posMod(thisEnum.ordinal + 1, ControlMode.entries.size)]
        val nextControls = controlModes[nextEnum] ?: this
        (uiParent as SceneView).editControls = nextControls
        modeButton.text = nextEnum.name
    }

    var lastSpaceTime = 0L
    var lastWTime = 0L

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_F4 -> nextMode()
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
            }
            else -> super.onKeyTyped(x, y, key)
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        inventoryUI.isVisible = false
    }

    override fun onEscapeKey(x: Float, y: Float) {
        inventoryUI.isVisible = false
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
        chunk.afterBlockChange(coords.x, coords.y, coords.z)
        return true
    }

    fun clickCast(): RayQuery? {
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = RayQuery(
            renderView.cameraPosition,
            renderView.mouseDirection,
            1e3
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