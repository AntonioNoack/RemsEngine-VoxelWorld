package me.anno.minecraft.multiplayer

import me.anno.Engine
import me.anno.Time
import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.minecraft.entity.PlayerEntity
import kotlin.concurrent.thread

fun main() {

    ECSRegistry.init()

    val t0 = Time.nanoTime
    val duration = 10 * 1e9
    for (i in 0 until 10) {
        val name = "Player[$i]"
        thread(name = name) {
            val player = PlayerEntity(true, name)
            val entities = Entity()
            while ((Time.nanoTime - t0) < duration) {
                MCProtocol.updatePlayers(player, entities)
                Thread.sleep(50)
            }
            Engine.requestShutdown()
        }
    }

}