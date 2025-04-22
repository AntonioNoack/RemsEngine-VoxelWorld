package me.anno.minecraft.ui

import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.posMod
import me.anno.minecraft.block.BlockRegistry
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.block.Metadata
import me.anno.minecraft.entity.Player
import me.anno.minecraft.rendering.v2.*
import me.anno.minecraft.world.Dimension
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toFloat
import me.anno.utils.types.Floats.toDegrees
import org.joml.AABBi
import org.joml.Vector3d
import org.joml.Vector3i
import kotlin.math.floor
import kotlin.math.max

abstract class MinecraftControls(
    val player: Player,
    val dimension: Dimension,
    val chunkLoader: ChunkLoader,
    renderer: RenderView
) : ControlScheme(renderer) {

    companion object {

        var inHandSlot = 0

        val inHand get() = inventory.slots[inHandSlot]
        val inHandItem get() = inHand.type

        val inventory = Inventory(9)
        val offHand = Inventory(1)

        val clickDistanceDelta = 0.001

        var lookedAtInventory: Inventory? = null
    }

    val modeButton = TextButton(NameDesc("Next Mode"), style)

    init {
        for ((i, type) in listOf(
            BlockRegistry.Dirt, BlockRegistry.Grass, BlockRegistry.Water, BlockRegistry.Lava,
            BlockRegistry.Sand, BlockRegistry.Sandstone
        ).withIndex()) {
            val slot = inventory.slots.getOrNull(i) ?: break
            slot.set(type, 1, null)
        }
        val inventoryBar = PanelListX(style)
        inventoryBar.add(ItemPanel(offHand.slots[0], Int.MAX_VALUE))
        inventoryBar.add(SpacerPanel(10, 0, style))
        for ((i, slot) in inventory.slots.withIndex()) {
            inventoryBar.add(ItemPanel(slot, i))
        }
        inventoryBar.alignmentX = AxisAlignment.CENTER
        inventoryBar.alignmentY = AxisAlignment.MAX
        add(inventoryBar)

        modeButton.addLeftClickListener { nextMode() }
        modeButton.alignmentX = AxisAlignment.MAX
        modeButton.alignmentY = AxisAlignment.MIN
        add(modeButton)

        // set initial rotation
        rotatePlayer(0f, 0f)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // todo if is in menu (inventory), handle super
        if (lookedAtInventory != null) {
            super.onMouseMoved(x, y, dx, dy)
        } else if (Input.isRightDown) { // todo always do it...
            rotatePlayer(dx, dy)
        }
    }

    fun rotatePlayer(dx: Float, dy: Float) {
        val speed = 1f / max(width, height)
        player.headRotationX = clamp(player.headRotationX + dy * speed, -PIf, PIf)
        player.bodyRotationY = (player.bodyRotationY + dx * speed) % TAUf
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

    fun updateMoveIntend() {
        player.moveIntend.set(
            checkKeys(Key.KEY_D, Key.KEY_ARROW_RIGHT) - checkKeys(Key.KEY_A, Key.KEY_ARROW_LEFT),
            Input.isKeyDown(Key.KEY_SPACE).toFloat() - Input.isControlDown.toFloat(),
            checkKeys(Key.KEY_S, Key.KEY_ARROW_DOWN) - checkKeys(Key.KEY_W, Key.KEY_ARROW_UP)
        ).rotateY(player.bodyRotationY).mul(1f, 30f, 1f)
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

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_F4 -> nextMode()
            else -> super.onKeyTyped(x, y, key)
        }
    }

    fun getCoords(query: RayQuery, delta: Double): Vector3i {
        val pos = query.result.positionWS
        val dir = query.direction
        dir.mulAdd(delta, pos, pos)
        return Vector3i(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
    }

    fun coordsToChunkId(coords: Vector3i): Vector3i {
        return Vector3i(
            coords.x shr dimension.bitsX,
            coords.y shr dimension.bitsY,
            coords.z shr dimension.bitsZ
        )
    }

    fun getBlock(coords: Vector3i): BlockType {
        return dimension.getBlockAt(coords.x, coords.y, coords.z)
    }

    fun getBlockMetadata(coords: Vector3i): Metadata? {
        return dimension.getMetadataAt(coords.x, coords.y, coords.z)
    }

    fun setBlock(coords: Vector3i, block: BlockType) {
        dimension.setElementAt(coords.x, coords.y, coords.z, true, block)
        val chunkId = coordsToChunkId(coords)
        // todo chunk invalidation is extremely slow
        // todo when setting blocks, we can temporarily place a block until the mesh has been recalculated
        invalidateChunk(chunkId)
        val localCoords = Vector3i(
            coords.x and dimension.maskX,
            coords.y and dimension.maskY,
            coords.z and dimension.maskZ
        )
        // when we're on the edge, and we remove a block (set a transparent one), we need to invalidate our neighbors, too
        if (!block.isSolid) {
            if (localCoords.x == 0) invalidateChunk(Vector3i(chunkId).sub(1, 0, 0))
            else if (localCoords.x == csx - 1) invalidateChunk(Vector3i(chunkId).add(1, 0, 0))
            if (localCoords.y == 0) invalidateChunk(Vector3i(chunkId).sub(0, 1, 0))
            else if (localCoords.y == csy - 1) invalidateChunk(Vector3i(chunkId).add(0, 1, 0))
            if (localCoords.z == 0) invalidateChunk(Vector3i(chunkId).sub(0, 0, 1))
            else if (localCoords.z == csz - 1) invalidateChunk(Vector3i(chunkId).add(0, 0, 1))
        }
        saveSystem.get(chunkId) { changesInChunk ->
            changesInChunk[localCoords] = block.id
            saveSystem.put(chunkId, changesInChunk)
        }
    }

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

    fun clickCast(): RayQuery? {
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = RayQuery(
            renderView.cameraPosition,
            Vector3d(renderView.mouseDirection),
            1e3
        )

        val queryBounds = AABBi()
        // todo these bounds aren't working correctly... why???
        // queryBounds.union(query.start.x.toInt(), query.start.y.toInt(), query.start.z.toInt())
        // queryBounds.addMargin(ceil(query.result.distance + 1.0).toInt())
        queryBounds.all()

        // todo we no longer need the meshes... where can we throw them away?
        val hitSomething =
            BlockTracing.blockTrace(query, (query.result.distance * 3).toInt(), queryBounds) { xi, yi, zi ->
                if (dimension.getBlockAt(xi, yi, zi, Int.MAX_VALUE).isSolid) BlockTracing.SOLID_BLOCK
                else BlockTracing.AIR_BLOCK
            }

        return if (hitSomething) query else null
    }

}