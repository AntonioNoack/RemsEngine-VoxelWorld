package me.anno.minecraft.ui.controls

import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.world.Dimension

class AdventureControls(sceneView: SceneView, player: PlayerEntity, dimension: Dimension, renderer: RenderView) :
    SurvivalControls(sceneView, player, dimension, renderer, false) {

    override val canFly: Boolean
        get() = false

}