package me.anno.minecraft.entity.ai

import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths.sq
import me.anno.maths.paths.NodeDistance
import me.anno.maths.paths.PathFinding
import me.anno.minecraft.entity.ai.FindTargets.canStandAt
import me.anno.ui.UIColors
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// todo find a certain target
// todo store the state to find there
// todo give an approximation for speed? if stuck, re-trace path
// todo timeout / thinking if nothing was found AND after something was found

class PathFinding(val halfExtents: Vector3f) {

    companion object {
        private val impl = PathFinding<Vector3i>()
        private val distance = NodeDistance<Vector3i> { a, b ->
            val dx = a.x - b.x
            val dy = (a.y - b.y) * 3 // avoid up/down
            val dz = a.z - b.z
            sq(dx, dy, dz).toDouble()
        }
        private const val SHIFT10 = 32 - 10
        private const val MASK10 = 1.shl(10) - 1
        private const val INVALID = 10

        fun aStarHelper(
            start: Vector3i, end: Vector3i,
            queryForward: (from: Vector3i, (Vector3i) -> Unit) -> Unit
        ): List<Vector3i>? {
            val maxDistance = distance.get(start, end) * 2.0 + 16.0
            return impl.aStarWithCallback(
                start, end, distance.get(start, end), maxDistance,
                includeStart = false, includeEnd = true,
            ) { from, callback ->
                queryForward(from) { to ->
                    // ensure coordinates can be encoded properly
                    if (abs(start.x - to.x) < 512 &&
                        abs(start.y - to.y) < 512 &&
                        abs(start.z - to.z) < 512
                    ) {
                        callback.respond(to, distance.get(from, to), distance.get(to, end))
                    }
                }
            }
        }

        fun aStarHelper(
            start: Vector3i, end: Vector3i,
            height: Int, maxJumpDown: Int
        ): List<Vector3i>? {
            return aStarHelper(start, end) { (x, y, z), callback ->
                // check +x,-x,+z,-z,+y,-y
                // check corners, if available for more natural movement
                val px = findStandAtY(x + 1, y, z, height, maxJumpDown)
                val pz = findStandAtY(x, y, z + 1, height, maxJumpDown)
                val nx = findStandAtY(x - 1, y, z, height, maxJumpDown)
                val nz = findStandAtY(x, y, z - 1, height, maxJumpDown)
                if (px != INVALID) callback(Vector3i(x + 1, y + px, z))
                if (pz != INVALID) callback(Vector3i(x, y + pz, z + 1))
                if (nx != INVALID) callback(Vector3i(x - 1, y + nx, z))
                if (nz != INVALID) callback(Vector3i(x, y + nz, z - 1))
                if (px == 0 && pz == 0 && canStandAt(x + 1, y, z + 1, height)) {
                    callback(Vector3i(x + 1, y, z + 1))
                }
                if (px == 0 && nz == 0 && canStandAt(x + 1, y, z - 1, height)) {
                    callback(Vector3i(x + 1, y, z - 1))
                }
                if (nx == 0 && pz == 0 && canStandAt(x - 1, y, z + 1, height)) {
                    callback(Vector3i(x - 1, y, z + 1))
                }
                if (nx == 0 && nz == 0 && canStandAt(x - 1, y, z - 1, height)) {
                    callback(Vector3i(x - 1, y, z - 1))
                }
                // todo add penalties for burning blocks, slow blocks and such
            }
        }

        fun findStandAtY(x: Int, y: Int, z: Int, height: Int, maxJumpDown: Int): Int {
            if (canStandAt(x, y, z, height)) return 0
            if (canStandAt(x, y + 1, z, height)) return 1
            for (dy in 1..maxJumpDown) {
                if (canStandAt(x, y - dy, z, height)) return -dy
            }
            return INVALID
        }

        private fun getS10(x: Int): Int {
            return (x shl SHIFT10) shr SHIFT10
        }
    }

    val start = Vector3i()
    val path = IntArrayList() // dx,dy,dz, each 10 bits signed
    var index = 0

    fun findPathTo(
        start: Vector3i, end: Vector3i,
        height: Int, maxJumpDown: Int
    ): Boolean {
        val path = aStarHelper(start, end, height, maxJumpDown)
            ?: return false
        setPath(start, path)
        return true
    }

    fun getXi(node: Int): Int {
        return getS10(node) + start.x
    }

    fun getYi(node: Int): Int {
        return getS10(node shr 10) + start.y
    }

    fun getZi(node: Int): Int {
        return getS10(node shr 20) + start.z
    }

    fun getX(node: Int) = getXi(node) + 0.5
    fun getY(node: Int) = getYi(node) + halfExtents.y.toDouble()
    fun getZ(node: Int) = getZi(node) + 0.5

    fun pack(x: Int, y: Int, z: Int): Int {
        val dx = (x - start.x) and MASK10
        val dy = (y - start.y) and MASK10
        val dz = (z - start.z) and MASK10
        return dx + dy.shl(10) + dz.shl(20)
    }

    fun setPath(start: Vector3i, src: List<Vector3i>): Boolean {
        this.start.set(start)
        index = 0
        val dst = this.path
        dst.size = 0
        dst.ensureCapacity(src.size)
        for (i in src.indices) {
            val node = src[i]
            dst.add(pack(node.x, node.y, node.z))
        }
        return true
    }

    /**
     * from index to min(size,index+3), find the index with the minimum distance
     * */
    fun nextTarget(position: Vector3d): Int {
        val startIndex = index
        var bestIndex = path.size
        var bestDistance = Double.MAX_VALUE
        val i0 = max(startIndex - 1, 0)
        val i1 = min(path.size, startIndex + 3)
        for (i in i0 until i1) {
            val pos = path[i]
            val dx = position.x - getX(pos)
            val dy = position.y - getY(pos)
            val dz = position.z - getZ(pos)
            val distance = dx * dx + dy * dy + dz * dz
            if (distance < bestDistance) {
                bestIndex = i
                bestDistance = distance
            }
        }
        if (bestDistance > 16.0) {
            // if distance is greater than threshold, give up
            return -1
        }
        this.index = bestIndex
        val nextIndex = bestIndex + 1
        return if (nextIndex < path.size) path[nextIndex] else -1
    }

    fun debugDraw() {
        // draw nodes and lines
        val color = UIColors.dodgerBlue.withAlpha(120)
        val vertices = path.indices.map { index ->
            val node = path[index]
            Vector3d(getX(node), getY(node), getZ(node))
        }
        for (i in vertices.indices) {
            val pos = vertices[i]
            DebugShapes.showDebugPoint(DebugPoint(pos, color, 0f))
            if (i > 0) DebugShapes.showDebugArrow(DebugLine(vertices[i - 1], pos, color, 0f))
        }
    }

}