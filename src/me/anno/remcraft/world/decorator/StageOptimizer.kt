package me.anno.remcraft.world.decorator

import me.anno.remcraft.world.Chunk
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager

object StageOptimizer {

    private val LOGGER = LogManager.getLogger(StageOptimizer::class)

    private class MergedDecorator(val stages: List<Decorator>, val i0: Int, val i1: Int) : Decorator {
        override fun decorate(chunk: Chunk) {
            for (i in i0 until i1) {
                stages[i].decorate(chunk)
            }
        }

        override val readsPreviousStage: Boolean get() = true
    }

    private val voidDecorator = MergedDecorator(emptyList(), 0, 0)

    fun optimizeStages(stages: List<Decorator>): List<Decorator> {
        if (stages.isEmpty()) return listOf(voidDecorator)
        if (stages.any2 { it is MergedDecorator }) return stages

        val result = ArrayList<Decorator>()
        var i = 0
        while (i < stages.size) {
            val i0 = i++
            while (i < stages.size && !stages[i].readsPreviousStage) {
                i++
            }
            val merged = if (i0 == i + 1) stages[i0] else MergedDecorator(stages, i0, i)
            result.add(merged)
        }
        return if (result.size < stages.size) {
            LOGGER.info("Compacted ${stages.size} into ${result.size} stages")
            result
        } else {
            stages
        }
    }
}