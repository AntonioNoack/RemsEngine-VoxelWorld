package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.minecraft.entity.Player
import me.anno.minecraft.rendering.v2.ChunkLoader
import me.anno.minecraft.world.Dimension

class AdventureControls(player: Player, dimension: Dimension, chunkLoader: ChunkLoader, renderer: RenderView) :
    SurvivalControls(player, dimension, chunkLoader, renderer, false)