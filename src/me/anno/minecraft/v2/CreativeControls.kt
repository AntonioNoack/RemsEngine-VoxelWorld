package me.anno.minecraft.v2

import me.anno.ecs.Entity
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.input.Key
import me.anno.maths.Maths.posMod
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.ui.Inventory
import me.anno.minecraft.ui.ItemPanel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3i
import kotlin.math.floor

class CreativeControls(
    val scene: Entity,
    val chunkLoader: ChunkLoader,
    renderer: RenderView
) : DraggingControls(renderer) {

    companion object {
        var inHandSlot = 0
        var inHandBlock
            get() = inventory.slots[inHandSlot].type
            set(value) {
                inventory.slots[inHandSlot].type = value
            }

        val inventory = Inventory(9)
        val offHand = Inventory(1)
    }

    init {
        for ((i, type) in listOf(
            BlockType.Dirt, BlockType.Grass, BlockType.Water, BlockType.Lava,
            BlockType.Sand, BlockType.Sandstone
        ).withIndex()) {
            val slot = inventory.slots.getOrNull(i) ?: break
            slot.type = type
        }
        val inventoryBar = PanelListX(style)
        inventoryBar.add(ItemPanel(offHand.slots[0], -1))
        inventoryBar.add(SpacerPanel(10, 0, style))
        for ((i, slot) in inventory.slots.withIndex()) {
            inventoryBar.add(ItemPanel(slot, i))
        }
        inventoryBar.alignmentX = AxisAlignment.CENTER
        inventoryBar.alignmentY = AxisAlignment.MAX
        add(inventoryBar)
    }

    fun getCoords(query: RayQuery, delta: Double): Vector3i {
        val pos = query.result.positionWS
        val dir = query.direction
        dir.mulAdd(delta, pos, pos)
        return Vector3i(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
    }

    fun setBlock(coords: Vector3i, block: BlockType) {
        world.setElementAt(coords.x, coords.y, coords.z, true, block)
        val chunkId = coordsToChunkId(coords)
        // todo chunk invalidation is extremely slow
        invalidateChunk(chunkId)
        val localCoords = Vector3i(
            posMod(coords.x, csx),
            posMod(coords.y, csy),
            posMod(coords.z, csz),
        )
        // when we're on the edge, and we remove a block (set a transparent one), we need to invalidate our neighbors, too
        if (block.isTransparent) {
            if (localCoords.x == 0) invalidateChunk(Vector3i(chunkId).sub(1, 0, 0))
            else if (localCoords.x == csx - 1) invalidateChunk(Vector3i(chunkId).add(1, 0, 0))
            if (localCoords.y == 0) invalidateChunk(Vector3i(chunkId).sub(0, 1, 0))
            else if (localCoords.y == csy - 1) invalidateChunk(Vector3i(chunkId).add(0, 1, 0))
            if (localCoords.z == 0) invalidateChunk(Vector3i(chunkId).sub(0, 0, 1))
            else if (localCoords.z == csz - 1) invalidateChunk(Vector3i(chunkId).add(0, 0, 1))
        }
        saveSystem.get(chunkId, true) { changesInChunk ->
            changesInChunk[localCoords] = block.id
            saveSystem.put(chunkId, changesInChunk)
        }
    }

    fun coordsToChunkId(coords: Vector3i): Vector3i {
        return Vector3i(
            coords.x shr world.bitsX,
            coords.y shr world.bitsY,
            coords.z shr world.bitsZ
        )
    }

    private val invalidChunks = HashSet<Vector3i>()
    fun invalidateChunk(coords: Vector3i) {
        val needsWorker = synchronized(invalidChunks) {
            invalidChunks.add(coords)
        }
        if(needsWorker) {
            chunkLoader.worker += {
                val changed = synchronized(invalidChunks) {
                    invalidChunks.remove(coords)
                }
                assertTrue(changed)
                chunkLoader.generateChunk(coords)
            }
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = RayQuery(
            renderView.cameraPosition,
            renderView.getMouseRayDirection(),
            1e3
        )
        // todo also implement cheaper raytracing (to show how) going block by block
        //  then we can throw away the meshes and save even more memory :3
        val hitSomething = Raycast.raycastClosestHit(scene, query)
        val delta = 0.001
        if (hitSomething) {
            when (button) {
                Key.BUTTON_LEFT -> {
                    // remove block
                    val coords = getCoords(query, +delta)
                    setBlock(coords, BlockType.Air)
                }
                Key.BUTTON_RIGHT -> {
                    // add block
                    val coords = getCoords(query, -delta)
                    setBlock(coords, inHandBlock)
                }
                Key.BUTTON_MIDDLE -> {
                    // get block
                    val coords = getCoords(query, +delta)
                    inHandBlock = world.getBlockAt(coords.x, coords.y, coords.z, -1)
                }
                else -> {}
            }
        }
    }
}