package me.anno.minecraft

import me.anno.engine.RemsEngine
import me.anno.extensions.ExtensionLoader
import me.anno.extensions.mods.Mod
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.minecraft.entity.Player
import me.anno.minecraft.entity.PlayerController
import me.anno.minecraft.visual.VisualChunk
import me.anno.minecraft.visual.VisualDimension

class MinecraftMod : Mod() {

    override fun onPreInit() {
        super.onPreInit()
        registerCustomClass(Player())
        registerCustomClass(PlayerController())
        registerCustomClass(VisualChunk())
        registerCustomClass(VisualDimension())
    }

    companion object {

        //////////////////////////////////////////
        // world: select, then invalidate aabbs //
        //////////////////////////////////////////

        // this is a test for multiple things
        //  - chunk system
        //  - procedural meshes
        //  - perlin generator
        //  - vox cube generator (might change)
        //  - parts of ecs, ui, rendering, ...

        // todo multiplayer test
        // todo ui
        // todo save world
        //  - absolute
        //  - delta
        // todo commands & console
        // todo set & mine blocks
        // todo entities

        @JvmStatic
        fun main(args: Array<String>) {
            ExtensionLoader.loadMainInfo()
            RemsEngine().run()
        }

    }

}