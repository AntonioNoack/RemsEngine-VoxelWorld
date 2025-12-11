package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.rendering.v2.ChunkLoader
import me.anno.minecraft.world.Dimension

class SpectatorControls(player: PlayerEntity, dimension: Dimension, chunkLoader: ChunkLoader, renderer: RenderView) :
    CreativeControls(player, dimension, chunkLoader, renderer)