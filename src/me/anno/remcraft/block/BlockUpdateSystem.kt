package me.anno.remcraft.block

import me.anno.Time
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.remcraft.rendering.v2.chunkLoader
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.world.Index.totalSize
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
        for (vec in chunkLoader.loadedChunks) {
            val chunk = dimension.getChunkOrNull(vec.x, vec.y, vec.z, Int.MAX_VALUE)?.value ?: continue
            repeat(numUpdates) {
                chunk.blockUpdates.add(Random.nextInt(totalSize))
            }
            chunk.processBlockUpdates()
        }
    }
}