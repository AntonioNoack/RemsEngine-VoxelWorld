package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.rendering.v2.ChunkLoader
import me.anno.minecraft.world.Dimension

class AdventureControls(player: PlayerEntity, dimension: Dimension, chunkLoader: ChunkLoader, renderer: RenderView) :
    SurvivalControls(player, dimension, chunkLoader, renderer, false) {

    override val canFly: Boolean
        get() = false

}