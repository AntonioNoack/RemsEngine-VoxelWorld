package me.anno.remcraft.block

import me.anno.Time
import me.anno.ecs.System
import me.anno.ecs.systems.OnUpdate
import me.anno.remcraft.rendering.v2.ChunkIndex.decodeChunkIndex
import me.anno.remcraft.rendering.v2.chunkLoader
import me.anno.remcraft.rendering.v2.dimension
import me.anno.remcraft.world.Index.totalSize
import org.joml.Vector3i
import org.joml.Vector4i
import kotlin.math.max
import kotlin.random.Random

object BlockUpdateSystem : System(), OnUpdate {

    var updatesPerSecond = 1000f
    private var timeRemainder = 0f
    private val tmp = Vector3i()
    private val key = Vector4i()

    override fun onUpdate() {
        timeRemainder += Time.deltaTime.toFloat()
        val updatesPerSecond = updatesPerSecond
        val numUpdates = max(0f, timeRemainder * updatesPerSecond).toInt()
        if (numUpdates > 0) timeRemainder -= numUpdates / updatesPerSecond

        // todo only those, that are close to the player
        chunkLoader.loadedChunks.forEach { encoded ->
            val vec = decodeChunkIndex(encoded, tmp)
            key.set(vec.x, vec.y, vec.z, Int.MAX_VALUE)
            val chunk = dimension.getChunkIfLoaded(key)?.value
            if (chunk != null) {
                repeat(numUpdates) {
                    chunk.blockUpdates.add(Random.nextInt(totalSize))
                }
                chunk.processBlockUpdates()
            }
        }
    }
}