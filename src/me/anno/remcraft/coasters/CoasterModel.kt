package me.anno.remcraft.coasters

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.engine.DefaultAssets.flatCube
import me.anno.maths.Maths.mix
import me.anno.remcraft.entity.model.Model
import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4x3
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

// todo we want some sort of metal & wood texture on this...
class CoasterModel(strip: CoasterRail) : Model<CoasterRail>() {

    companion object {
        val profile0 = listOf(
            Vector2f(5f, 0f),
            Vector2f(6f, 0f),
            Vector2f(6f, -1f),
            Vector2f(5f, -1f),
        ).apply { forEach { it.mul(1f / 16f) } }

        val profile1 = profile0.reversed()
            .map { Vector2f(-it.x, it.y) }

        val profiles = listOf(profile0, profile1)

        fun buildTransforms(strip: CoasterRail): List<Matrix4x3> {

            val p0 = Vector3d()
            val p2 = Vector3d()

            strip.getPointAt(0.4, p0)
            strip.getPointAt(0.6, p2)

            val middleDir = p2.sub(p0).normalize()
            val totalAngle = strip.from.direction.angle(middleDir) +
                    middleDir.angle(strip.to.direction)

            val numSegments = ceil(2 + totalAngle * 10.0).toInt()
            return List(numSegments) { idx ->
                val t0 = idx / (numSegments - 1.0)
                val ti = strip.mapper[t0]
                strip.getTransformAt(ti, Matrix4x3())
            }
        }

        fun getLength(t0: Matrix4x3, t1: Matrix4x3): Double {
            return Vector3d.length(t0.m30 - t1.m30, t0.m31 - t1.m31, t0.m32 - t1.m32)
        }

        fun getLength(transforms: List<Matrix4x3>): Double {
            var totalLength = 0.0
            for (i in 1 until transforms.size) {
                totalLength += getLength(transforms[i - 1], transforms[i])
            }
            return totalLength
        }

        fun buildMesh(strip: CoasterRail): Mesh {
            val mesh = Mesh()
            val transforms = buildTransforms(strip)
            val offset = strip.center.negate(Vector3d())
            for (i in transforms.indices) {
                transforms[i].translateLocal(offset)
            }

            val totalLength = getLength(transforms)
            val numSleepers = (totalLength * 3).roundToInt()

            var ctr = 0
            flatCube.forEachTriangle { a, b, c ->
                ctr++
                false
            }
            check(ctr == 6 * 2)

            println("transforms: ${transforms.size}, sleepers: $numSleepers")

            val numRailQuads = (transforms.size - 1) * profiles.sumOf { it.size }
            val numEndQuads = profiles.size * 2
            val numSleeperQuads = numSleepers * 6
            val numTriangles = (numRailQuads + numSleeperQuads + numEndQuads) * 2
            val positions = FloatArray(numTriangles * 9)

            val tmp0 = Vector3f()
            val tmp1 = Vector3f()

            var k = 0
            fun put(p: Vector2f, t: Matrix4x3) {
                t.transformPosition(tmp0.set(p.x, p.y, 0f))
                tmp0.get(positions, k)
                k += 3
            }

            for (profile in profiles) {
                for (ti in 1 until transforms.size) {
                    val t0 = transforms[ti - 1]
                    val t1 = transforms[ti]

                    for (j in profile.indices) {
                        val j1 = (j + 1) % profile.size
                        val pj0 = profile[j]
                        val pj1 = profile[j1]
                        put(pj0, t0)
                        put(pj1, t1)
                        put(pj0, t1)

                        put(pj0, t0)
                        put(pj1, t0)
                        put(pj1, t1)
                    }
                }

                // put end-pieces
                val t0 = transforms.first()
                put(profile[0], t0)
                put(profile[2], t0)
                put(profile[1], t0)
                put(profile[0], t0)
                put(profile[3], t0)
                put(profile[2], t0)

                val t1 = transforms.last()
                put(profile[0], t1)
                put(profile[1], t1)
                put(profile[2], t1)
                put(profile[0], t1)
                put(profile[2], t1)
                put(profile[3], t1)
            }
            assertEquals((numRailQuads + numEndQuads) * 2 * 9, k)

            for (i in 0 until numSleepers) {
                var tf = ((i + 0.5) * (transforms.size - 1)) / numSleepers
                val ti = min(tf.toInt(), transforms.size - 2)
                val t0 = transforms[ti]
                val t1 = transforms[ti + 1]
                tf -= ti

                val x = mix(t0.m30, t1.m30, tf).toFloat()
                val y = mix(t0.m31, t1.m31, tf).toFloat()
                val z = mix(t0.m32, t1.m32, tf).toFloat()

                fun add(v: Vector3f) {
                    tmp0.set(v); tmp0.y -= 3f
                    tmp0.mul(7.0f / 16f, 0.5f / 16f, 1f / 16f)
                    tmp1.set(tmp0)
                    t0.transformDirection(tmp0)
                    t1.transformDirection(tmp1)
                    tmp0.mix(tmp1, tf.toFloat())

                    positions[k++] = x + tmp0.x
                    positions[k++] = y + tmp0.y
                    positions[k++] = z + tmp0.z
                }

                flatCube.forEachTriangle { a, b, c ->
                    add(a)
                    add(c)
                    add(b)
                    false
                }
            }

            assertEquals(positions.size, k)
            mesh.positions = positions
            return mesh
        }
    }

    val mesh = buildMesh(strip)

    override fun fill(
        transform: Transform,
        callback: (IMesh, Transform) -> Unit
    ) {
        callback(mesh, transform)
    }
}
