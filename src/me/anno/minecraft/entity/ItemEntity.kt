package me.anno.minecraft.entity

import me.anno.Time
import me.anno.minecraft.entity.model.ItemModel
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.ui.ItemSlot
import me.anno.utils.OS.res
import org.joml.Vector3f

// todo 2d/3d depending on whether it is an item
// todo cache the models...
// todo when we have all blocks on a texture, we should pack all items on one, too

class ItemEntity(val stack: ItemSlot) : MovingEntity(halfExtents, blockTexture) {

    companion object {
        private val halfExtents = Vector3f(0.2f)
        private val blockTexture = Texture(res.getChild("textures/blocks/Blocks.png"))
    }

    var spawnTime = Time.gameTime

    override val model: Model<*>
        get() = ItemModel

}