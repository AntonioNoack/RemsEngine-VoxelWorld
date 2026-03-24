package me.anno.remcraft.entity

import me.anno.Time
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.remcraft.block.BlockColor.NUM_TEX_X
import me.anno.remcraft.block.BlockColor.NUM_TEX_Y
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.entity.model.CuboidCreator
import me.anno.remcraft.entity.model.ItemModel
import me.anno.remcraft.entity.model.Model
import me.anno.remcraft.entity.model.PlaneCreator
import me.anno.remcraft.item.ItemType
import me.anno.remcraft.ui.ItemSlot
import me.anno.utils.OS.res
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3f

class ItemEntity(val stack: ItemSlot) : MovingEntity(halfExtents, blockTexture) {
    @Suppress("unused")
    constructor() : this(ItemSlot())

    companion object {
        private val halfExtents = Vector3f(0.2f)
        private val blockTexture = Texture(res.getChild("textures/blocks/Blocks.png"))
        private val models = LazyMap { type: ItemType ->
            val mesh = if (type is BlockType) {
                CuboidCreator.createMonoCuboid(
                    16, 16, 16,
                    type.texX * 16, type.texY * 16,
                    getSize(NUM_TEX_X * 16, NUM_TEX_Y * 16),
                    halfExtents.x
                )
            } else {
                // todo get item texture, item id etc...
                PlaneCreator.createPlane(
                    1, 1,
                    type.texX, type.texY,
                    getSize(NUM_TEX_X, NUM_TEX_Y)
                )
            }
            ItemModel(mesh)
        }
    }

    var spawnTime = Time.gameTime

    override val model: Model<*>
        get() = models[stack.type]

}