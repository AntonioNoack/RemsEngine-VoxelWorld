package me.anno.remcraft.coasters

import me.anno.io.files.InvalidRef
import me.anno.maths.optimization.GoldenSectionSearch.minimizeFunction
import me.anno.remcraft.entity.RemcraftEntity
import me.anno.remcraft.entity.Texture
import me.anno.remcraft.entity.model.Model
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

// todo ui to connect two coaster stands...
// todo when placed, place linked-blocks (for holding the space free) on the strip to block building; mainly above, below can be used for decorations
// todo when a minecart is in a curve, let it tilt; if too much tilt, derail

class CoasterRail(val from: CoasterRailBase, val to: CoasterRailBase) :
    RemcraftEntity(Vector3f(3f), Texture(InvalidRef)) {

    val distance = 0.5f * from.position.distance(to.position) +
            (1f - from.direction.dot(to.direction)) // plus factor for curve

    val p0 get() = from.position
    val p1 = from.position + from.direction * distance
    val p2 = to.position - to.direction * distance
    val p3 get() = to.position

    val mapper = EquiSpline(this)
    val center = Vector3d()

    init {
        p0.min(p1, minPosition).min(p2).min(p3).sub(0.5)
        p0.max(p1, maxPosition).max(p2).max(p3).add(0.5)

        maxPosition.sub(minPosition, halfExtents).mul(0.5f)
        minPosition.add(maxPosition, center).mul(0.5)
    }

    fun getPointAt(t: Double, dst: Vector3d): Vector3d {
        val b = 1.0 - t
        val b2 = b * b
        val t2 = t * t
        val c0 = b * b2
        val c1 = 3.0 * t * b2
        val c2 = 3.0 * t2 * b
        val c3 = t2 * t
        return interpolate(c0, c1, c2, c3, dst)
    }

    fun getDirectionAt(t: Double, dst: Vector3d): Vector3d {
        val b = 1.0 - t
        val b2 = b * b
        val t2 = t * t
        val bt2 = 2.0 * b * t

        // factor of 3 was removed, because result will be normalized anyway
        val c1 = b2 - bt2
        val c2 = bt2 - t2
        return interpolate(-b2, c1, c2, t2, dst)
    }

    fun interpolate(c0: Double, c1: Double, c2: Double, c3: Double, dst: Vector3d): Vector3d {
        return p0.mul(c0, dst)
            .add(p1.x * c1, p1.y * c1, p1.z * c1)
            .add(p2.x * c2, p2.y * c2, p2.z * c2)
            .add(p3.x * c3, p3.y * c3, p3.z * c3)
    }

    fun getTransformAt(t: Double, dst: Matrix4x3): Matrix4x3 {

        val y0 = from.upDirection
        val y1 = to.upDirection

        val z = getDirectionAt(t, Vector3d()).normalize()
        val y = Vector3d()
        val x = Vector3d()

        // estimate y-axis as the average between the values... not ideal...
        // todo implement a good y-axis, if the curve makes a 180° turn down/up-wards...
        y0.mix(y1, t, y)
        if (y.lengthSquared() < 0.01) {
            y.set(0.0, 1.0, 0.0)
        }

        y.fma(-z.dot(y), z)
        // todo y may have become 0, again...
        y.normalize()
        z.cross(y, x)

        val pos = getPointAt(t, Vector3d())
        return dst.set(
            x.x.toFloat(), x.y.toFloat(), x.z.toFloat(),
            y.x.toFloat(), y.y.toFloat(), y.z.toFloat(),
            z.x.toFloat(), z.y.toFloat(), z.z.toFloat(),
            pos.x, pos.y, pos.z,
        )
    }

    fun findClosestT(pos: Vector3d): Double {
        val ref = mapper.positions
        val closestPos = ref.minBy { refPos -> pos.distanceSquared(refPos) }
        val closestI = ref.indexOf(closestPos)
        val tmp = Vector3d()
        val minT = max((closestI - 0.67) / EquiSpline.n, -0.01)
        val maxT = min((closestI + 0.67) / EquiSpline.n, +1.01)
        return minimizeFunction(minT, maxT, 1e-5) { t -> getPointAt(t, tmp).distanceSquared(pos) }
    }

    override val model: Model<*> = CoasterModel(this)

}