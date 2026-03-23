package me.anno.remcraft.rendering.globalillumination.gpu

import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4i


fun sign(f: Float): Float = kotlin.math.sign(f)
fun sign(f: Double): Double = kotlin.math.sign(f)

fun sign(dir: Vector3f): Vector3f {
    return Vector3f(sign(dir.x), sign(dir.y), sign(dir.z))
}

fun ivec3(v: Vector3f): Vector3i {
    return Vector3i(
        v.x.toInt(),
        v.y.toInt(),
        v.z.toInt(),
    )
}

fun ivec3(x: Int, y: Int, z: Int): Vector3i {
    return Vector3i(x, y, z)
}

fun floor(v: Vector3f): Vector3f {
    return v.floor(Vector3f())
}

fun vec3(v: Vector3i): Vector3f {
    return Vector3f(v)
}

operator fun Vector3f.plus(v: Float): Vector3f {
    return Vector3f(x + v, y + v, z + v)
}

operator fun Vector3f.minus(v: Float): Vector3f {
    return Vector3f(x - v, y - v, z - v)
}

operator fun Vector3f.div(v: Vector3f): Vector3f {
    return Vector3f(x / v.x, y / v.y, z / v.z)
}

data class Vector3u(val x: UInt, val y: UInt, val z: UInt) {
    infix fun shr(o: Vector3i): Vector3u {
        return Vector3u(x shr o.x, y shr o.y, z shr o.z)
    }
}

infix fun Vector3i.shr(o: Vector3i): Vector3i {
    return Vector3i(x shr o.x, y shr o.y, z shr o.z)
}

fun uvec3(v: UInt): Vector3u {
    return Vector3u(v, v, v)
}

fun uvec3(v: Vector3i): Vector3u {
    return Vector3u(v.x.toUInt(), v.y.toUInt(), v.z.toUInt())
}

fun ivec4(v: Int): Vector4i = Vector4i(v)

fun ivec4(v: Vector3i, w: Int): Vector4i = Vector4i(v.x, v.y, v.z, w)