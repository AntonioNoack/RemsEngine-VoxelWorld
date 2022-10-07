package me.anno.minecraft.multiplayer

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.minecraft.entity.Player
import kotlin.concurrent.thread

fun main() {

    ECSRegistry.init()

    val t0 = Engine.nanoTime
    val duration = 10 * 1e9
    for (i in 0 until 10) {
        val name = "Player[$i]"
        thread(name = name) {
            val player = Player(true, name)
            val entities = Entity()
            while ((Engine.nanoTime - t0) < duration) {
                MCProtocol.updatePlayers(player, entities)
                Thread.sleep(50)
            }
            Engine.requestShutdown()
        }
    }

}