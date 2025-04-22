package me.anno.minecraft.ui

import me.anno.engine.ui.render.RenderView
import me.anno.input.Key
import me.anno.minecraft.block.BlockType
import me.anno.minecraft.entity.Player
import me.anno.minecraft.item.RightClickBlock
import me.anno.minecraft.item.RightClickItem
import me.anno.minecraft.rendering.v2.ChunkLoader
import me.anno.minecraft.world.Dimension

class SpectatorControls(player: Player, dimension: Dimension, chunkLoader: ChunkLoader, renderer: RenderView) :
    CreativeControls(player, dimension, chunkLoader, renderer)