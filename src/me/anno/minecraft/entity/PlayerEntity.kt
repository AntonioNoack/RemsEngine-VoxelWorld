package me.anno.minecraft.entity

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PlayerModel
import me.anno.minecraft.multiplayer.NetworkData
import me.anno.minecraft.ui.Inventory
import me.anno.minecraft.ui.ItemSlot
import me.anno.minecraft.ui.controls.GameMode
import me.anno.utils.OS.res
import org.joml.Vector3f
import org.joml.Vector3i

class PlayerEntity(var isPrimary: Boolean, name: String) : Animal(halfExtents, texture) {

    constructor() : this(false, "Gustav${(Math.random() * 1e6).toInt()}")

    companion object {
        // todo bug, why can we not enter 2-high caves???
        private val halfExtents = Vector3f(6f / 16f, 0.9f, 6f / 16f)
        private val femaleModel = PlayerModel(false)
        private val texture = Texture(res.getChild("textures/players/Reyviee.png"))

        const val MAIN_SLOTS = 0
        const val CHEST_SLOTS = MAIN_SLOTS + 1 * 9
        const val ARMOR_SLOTS = CHEST_SLOTS + 6 * 9
        const val OFF_HAND_SLOT = ARMOR_SLOTS + 4
        const val EDIT_SLOT = OFF_HAND_SLOT + 1
    }

    init {
        this.name = name
    }

    override val model: Model<*> get() = femaleModel
    override val maxJumpDown: Int get() = 3
    override val maxHealth: Int get() = 20

    val maxHunger = 20
    var hunger = maxHunger.toFloat()

    var usingLeftHand = false
    var usingRightHand = false

    var smoothLeft = 0f
    var smoothRight = 0f

    var inHandSlot = 0
    val inventory = Inventory(
        9 * (6 + 1) /* main + chest */ +
                4 /* armor */ +
                1 /* off-hand */ +
                1 /* edit-slot, dragged */
    )

    fun canBeAttacked() = gameMode.canBeAttacked()

    // idk what players would be looking for ;)
    override fun findTarget(start: Vector3i, seed: Long): Vector3i? = null

    val networkData = NetworkData()
    var gameMode = GameMode.CREATIVE
    var firstPersonMode = true
    var isSneaking = false

    var smoothAngle0 = 0f

    var experience = 0

    var targetHeadY = 0f

    fun addXp(value: Int) {
        experience += value
    }

    override fun addItemFrom(stack: ItemSlot): Boolean {
        return inventory.addItemFrom(stack)
    }

    override val className: String = "MCPlayer"

    override fun clone(): Component {
        val clone = PlayerEntity(isPrimary, name)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PlayerEntity
        dst.isPrimary = isPrimary
    }

}