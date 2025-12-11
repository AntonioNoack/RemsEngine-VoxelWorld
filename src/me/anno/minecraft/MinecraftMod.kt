package me.anno.minecraft

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.mods.Mod
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.minecraft.entity.PlayerEntity
import me.anno.minecraft.rendering.v1.VisualDimension

class MinecraftMod : Mod() {

    override fun onPreInit() {
        super.onPreInit()
        registerCustomClass(PlayerEntity())
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
        //  - creative
        //  - survival
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
        }

    }

}