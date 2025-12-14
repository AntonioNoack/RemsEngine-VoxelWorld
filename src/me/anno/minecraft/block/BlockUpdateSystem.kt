package me.anno.minecraft.block

import me.anno.Time
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.minecraft.rendering.v2.chunkLoader
import me.anno.minecraft.rendering.v2.dimension
import kotlin.math.max
import kotlin.random.Random

object BlockUpdateSystem : System(), OnUpdate {

    var updatesPerSecond = 1000f
    private var timeRemainder = 0f

    override fun onUpdate() {
        timeRemainder += Time.deltaTime.toFloat()
        val updatesPerSecond = updatesPerSecond
        val numUpdates = max(0f, timeRemainder * updatesPerSecond).toInt()
        if (numUpdates > 0) timeRemainder -= numUpdates / updatesPerSecond

        // todo only those, that are close to the player
        val chunkSize = dimension.sizeX * dimension.sizeY * dimension.sizeZ
        for (vec in chunkLoader.loadedChunks) {
            val chunk = dimension.getChunk(vec.x, vec.y, vec.z, -1) ?: continue
            repeat(numUpdates) {
                chunk.blockUpdates.add(Random.nextInt(chunkSize))
            }
            chunk.processBlockUpdates()
        }
    }
}