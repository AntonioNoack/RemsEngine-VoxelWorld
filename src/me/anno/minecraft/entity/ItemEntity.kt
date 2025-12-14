package me.anno.minecraft.entity

import me.anno.Time
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.entity.model.CuboidCreator
import me.anno.minecraft.entity.model.ItemModel
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PlaneCreator
import me.anno.minecraft.item.ItemType
import me.anno.minecraft.ui.ItemSlot
import me.anno.utils.OS.res
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3f

class ItemEntity(val stack: ItemSlot) : MovingEntity(halfExtents, blockTexture) {

    companion object {
        private val halfExtents = Vector3f(0.2f)
        private val blockTexture = Texture(res.getChild("textures/blocks/Blocks.png"))
        private val models = LazyMap { type: ItemType ->
            val mesh = if (type is BlockType) {
                CuboidCreator.createMonoCuboid(
                    16, 16, 16,
                    type.texId.and(15) * 16,
                    type.texId.shr(4) * 16,
                    getSize(256, 512),
                    halfExtents.x
                )
            } else {
                // todo get item texture, item id etc...
                PlaneCreator.createPlane(
                    1, 1,
                    type.texId.and(15),
                    type.texId.shr(4),
                    getSize(32, 64)
                )
            }
            ItemModel(mesh)
        }
    }

    var spawnTime = Time.gameTime

    override val model: Model<*>
        get() = models[stack.type]

}