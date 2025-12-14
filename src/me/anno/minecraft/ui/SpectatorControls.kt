package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.world.Dimension

class SpectatorControls(player: PlayerEntity, dimension: Dimension, renderer: RenderView) :
    CreativeControls(player, dimension, renderer)