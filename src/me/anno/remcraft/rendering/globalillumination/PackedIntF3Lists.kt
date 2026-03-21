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
        floats = FloatArray(totalCapacity * 3)
        clear()
    }

    fun addUnique(index: Int, value: Int, x: Float, y: Float, z: Float) {
        var pos = offsets[index]
        while (true) {
            val valueI = values[pos]
            if (valueI == invalidValue) {
                return add(index, value, x, y, z)
            }

            if (valueI == value) {
                val pos3 = pos * 3
                floats[pos3 + 0] += x
                floats[pos3 + 1] += y
                floats[pos3 + 2] += z
                return
            }
            pos++
        }
    }

    fun add(index: Int, value: Int, x: Float, y: Float, z: Float) {
        var index = index
        var value = value
        var x = x
        var y = y
        var z = z

        while (true) {
            if (index >= offsets.size) println("Illegal index! $index vs $size, ${offsets.size}")
            val pos = offsets[index] + getSize(index)
            val pos3 = pos * 3

            // check if next cell is free for end marker
            if (pos + 1 >= values.size) grow()

            val wouldBeOverridden = values[pos + 1]
            val wboX = floats[pos3 + 3]
            val wboY = floats[pos3 + 4]
            val wboZ = floats[pos3 + 5]

            // insert value and new end marker
            values[pos] = value
            floats[pos3 + 0] = x
            floats[pos3 + 1] = y
            floats[pos3 + 2] = z
            values[pos + 1] = invalidValue

            index++
            if (index < offsets.size && pos + 1 == offsets[index]) {
                // Need to move suffix forward (shift right until free space)
                offsets[index] = pos + 2
                value = wouldBeOverridden
                x = wboX
                y = wboY
                z = wboZ
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

    inline fun forEach(index: Int, callback: (Int, Float, Float, Float) -> Unit): Int {
        val pos0 = offsets[index]
        var pos = pos0
        while (true) {
            val value = values[pos]
            if (value == invalidValue) return pos - pos0
            val pos3 = pos * 3
            callback(value, floats[pos3], floats[pos3 + 1], floats[pos3 + 2])
            pos++
        }
    }

    inline fun forEach(callback: (index: Int, value: Int, Float, Float, Float) -> Unit) {
        for (index in 0 until size) {
            val pos0 = offsets[index]
            var pos = pos0
            while (true) {
                val value = values[pos]
                if (value == invalidValue) break
                val pos3 = pos * 3
                callback(index, value, floats[pos3], floats[pos3 + 1], floats[pos3 + 2])
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
        floats = floats.copyOf(newValues.size * 3)
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

    /**
     * Clears all values for this index
     * */
    fun clear(index: Int) {
        val size = getSize(index)
        val offset = offsets[index]
        for (i in 0 until size) {
            values[offset + i] = invalidValue
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
            floats = floats.copyOf(requiredSize * 3)
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