package me.anno.minecraft.entity

import me.anno.Time
import me.anno.minecraft.entity.model.ItemModel
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.ui.ItemSlot
import org.joml.Vector3f

class ItemEntity(val stack: ItemSlot) : MovingEntity(halfExtents) {

    companion object {
        private val halfExtents = Vector3f(0.2f)
        private val itemModel = ItemModel()
    }

    var spawnTime = Time.gameTime

    override val model: Model<*>
        get() = itemModel

}