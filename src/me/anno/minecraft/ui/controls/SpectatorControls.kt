package me.anno.minecraft.ui.controls

import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.world.Dimension

class SpectatorControls(sceneView: SceneView, player: PlayerEntity, dimension: Dimension, renderer: RenderView) :
    CreativeControls(sceneView, player, dimension, renderer)