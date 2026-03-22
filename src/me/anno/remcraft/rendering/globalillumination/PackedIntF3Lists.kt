package me.anno.remcraft.rendering.globalillumination

import me.anno.maths.Maths.ceilDiv
import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt
import kotlin.math.max

/**
 * Represents an Array<List<Int>>, with -1 being an invalid value.
 * Is much more memory friendly than Array<List<Int>>.
 *
 * Be wary that if you underestimate initialCapacityPerValue, this collect gets really slow!
 * The order of the inserted items may change.
 * */
class PackedIntF3Lists(
    var size: Int,
    initialCapacityPerValue: Int,
    val invalidValue: Int,
) {

    @InternalAPI
    var offsets: IntArray = IntArray(size)

    @InternalAPI
    var values: IntArray

    @InternalAPI
    var floats: FloatArray

    init {
        val initialCapacityPerValue = max(initialCapacityPerValue, 1)
        val totalCapacity = size * initialCapacityPerValue + (initialCapacityPerValue == 1).toInt()
        values = IntArray(totalCapacity)
        floats = FloatArray(totalCapacity * 6)
        clear()
    }

    fun addUnique(
        index: Int, value: Int,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
    ) {
        var pos = offsets[index]
        val values = values
        while (true) {
            val valueI = values[pos]
            if (valueI == invalidValue) {
                return add(index, value, x0, y0, z0, x1, y1, z1)
            }

            if (valueI == value) {
                val pos6 = pos * 6
                val floats = floats
                floats[pos6 + 0] += x0
                floats[pos6 + 1] += y0
                floats[pos6 + 2] += z0
                floats[pos6 + 3] += x1
                floats[pos6 + 4] += y1
                floats[pos6 + 5] += z1
                return
            }
            pos++
        }
    }

    fun add(
        index: Int, value: Int,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
    ) {
        var index = index
        var value = value
        var x0 = x0
        var y0 = y0
        var z0 = z0
        var x1 = x1
        var y1 = y1
        var z1 = z1

        val offsets = offsets
        while (true) {
            if (index >= offsets.size) println("Illegal index! $index vs $size, ${offsets.size}")
            val pos = offsets[index] + getSize(index)
            val pos6 = pos * 6

            // check if next cell is free for end marker
            if (pos + 1 >= values.size) grow()

            val values = values
            val wouldBeOverridden = values[pos + 1]
            val floats = floats
            val x2 = floats[pos6 + 6]
            val y2 = floats[pos6 + 7]
            val z2 = floats[pos6 + 8]
            val x3 = floats[pos6 + 9]
            val y3 = floats[pos6 + 10]
            val z3 = floats[pos6 + 11]

            // insert value and new end marker
            values[pos] = value
            floats[pos6 + 0] = x0
            floats[pos6 + 1] = y0
            floats[pos6 + 2] = z0
            floats[pos6 + 3] = x1
            floats[pos6 + 4] = y1
            floats[pos6 + 5] = z1
            values[pos + 1] = invalidValue

            index++
            if (index < offsets.size && pos + 1 == offsets[index]) {
                // Need to move suffix forward (shift right until free space)
                offsets[index] = pos + 2
                value = wouldBeOverridden
                x0 = x2
                y0 = y2
                z0 = z2
                x1 = x3
                y1 = y3
                z1 = z3
            } else break
        }
    }

    operator fun get(index: Int, index2: Int): Int {
        var pos = offsets[index]
        var count = 0
        while (values[pos] != invalidValue) {
            if (count == index2) return values[pos]
            pos++
            count++
        }
        throw IndexOutOfBoundsException("row=$index col=$index2")
    }

    fun interface ForEachCallback {
        fun call(
            index: Int, value: Int,
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
        )
    }

    fun forEach(callback: ForEachCallback) {
        for (index in 0 until size) {
            val pos0 = offsets[index]
            var pos = pos0
            while (true) {
                val value = values[pos]
                if (value == invalidValue) break
                val pos6 = pos * 6
                callback.call(
                    index, value,
                    floats[pos6 + 0], floats[pos6 + 1], floats[pos6 + 2],
                    floats[pos6 + 3], floats[pos6 + 4], floats[pos6 + 5],
                )
                pos++
            }
        }
    }

    fun contains(index: Int, value: Int): Boolean {
        var pos = offsets[index]
        while (true) {
            val valueI = values[pos]
            if (valueI == invalidValue) return false
            if (valueI == value) return true
            pos++
        }
    }

    fun getSize(index: Int): Int {
        var pos = offsets[index]
        var count = 0
        while (values[pos] != invalidValue) {
            count++
            pos++
        }
        return count
    }

    fun totalSize(): Int {
        var sum = 0
        for (i in 0 until size) {
            sum += getSize(i)
        }
        return sum
    }

    private fun grow() {
        val values = values
        val newValues = values.copyOf(values.size * 2)
        newValues.fill(invalidValue, values.size, newValues.size)
        this.values = newValues
        floats = floats.copyOf(newValues.size * 6)
    }

    /**
     * Clears all values for all indices
     * */
    fun clear() {

        // mark values as invalid
        values.fill(invalidValue)

        // distribute blocks evenly
        val factor = values.size.toLong().shl(32) / max(size, 1)
        for (row in 0 until size) {
            offsets[row] = (row * factor).shr(32).toInt()
        }
    }

    fun resizeTo(newSize: Int) {
        val oldSize = size
        val cellsPerSize = ceilDiv(values.size, oldSize)
        assertTrue(cellsPerSize >= 2)

        val oldNumValues = values.size
        val requiredSize = oldNumValues + (newSize - oldSize) * cellsPerSize

        if (requiredSize > oldNumValues) {
            values = values.copyOf(requiredSize)
            values.fill(invalidValue, oldNumValues, requiredSize)
            floats = floats.copyOf(requiredSize * 6)
        }

        offsets = offsets.copyOf(newSize)

        // define start offsets for the new cells
        for (i in oldSize until newSize) {
            offsets[i] = oldNumValues + (i - oldSize) * cellsPerSize + 1
        }
        if (oldSize in 1 until newSize) {
            assertEquals(values[offsets[oldSize] - 1], invalidValue)
            // cell before us must be free
        }

        size = newSize
    }
}