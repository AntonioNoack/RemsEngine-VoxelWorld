package me.anno.minecraft.entity

import me.anno.minecraft.entity.model.Model
import me.anno.minecraft.entity.model.PigModel
import me.anno.utils.OS.res
import org.joml.Vector3f

class BoarSkeletonEntity : Animal(halfExtents) {
    companion object {
        private val halfExtents = Vector3f(7f / 16f)
        private val boarModel = PigModel(res.getChild("textures/animals/BoarSkeleton.png"))
    }

    override val model: Model<*>
        get() = boarModel
}