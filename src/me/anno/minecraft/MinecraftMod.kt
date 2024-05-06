package me.anno.minecraft

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.mods.Mod
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.minecraft.entity.Player
import me.anno.minecraft.entity.PlayerController
import me.anno.minecraft.visual.VisualDimension

class MinecraftMod : Mod() {

    override fun onPreInit() {
        super.onPreInit()
        registerCustomClass(Player())
        registerCustomClass(PlayerController())
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
            Build.isDebug = false
            val scene = Entity("Scene")
            scene.addChild(VisualDimension())
            testSceneWithUI("Minecraft", scene)
            // todo game isn't shutting down properly... why???
        }

    }

}