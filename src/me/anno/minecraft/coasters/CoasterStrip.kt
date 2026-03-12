package me.anno.minecraft.coasters

import me.anno.io.files.InvalidRef
import me.anno.minecraft.entity.Entity
import me.anno.minecraft.entity.Texture
import me.anno.minecraft.entity.model.Model
import org.joml.Vector3d
import org.joml.Vector3f

// todo connect two coaster stands...
// todo when placed, place linked-blocks (for holding the space free) on the strip to block building; mainly above, below can be used for decorations
// todo when a minecart is in a curve, let it tilt; if too much tilt, derail

class CoasterStrip(val from: CoasterStand, val to: CoasterStand) :
    Entity(Vector3f(3f), Texture(InvalidRef)) {

    val distance = from.position.distance(to.position) +
            3f * (1f - from.direction.dot(to.position)) // plus factor for curve

    val p0 = from.position
    val p1 = p0 + from.direction * distance
    val p2 = to.position - to.direction * distance
    val p3 = to.position

    fun getPointAt(t: Double, dst: Vector3d): Vector3d {
        val b = 1.0 - t
        val b2 = b * b
        val pr2 = t * t
        val aaa = pr2 * t
        val aab = 3.0 * pr2 * b
        val abb = 3.0 * t * b2
        val bbb = b * b2
        return dst.set(p0).mul(bbb)
            .add(p1.x * abb, p1.y * abb, p1.z * abb)
            .add(p2.x * aab, p2.y * aab, p2.z * aab)
            .add(p3.x * aaa, p3.y * aaa, p3.z * aaa)
    }

    // todo create mesh on a cubic spline
    //  we need equi-distant bars
    //  we need the main bars

    override val model: Model<*>
        get() = TODO("Not yet implemented")

}