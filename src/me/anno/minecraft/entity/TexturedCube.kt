package me.anno.minecraft.entity

import me.anno.cache.FileCacheList
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.Vector2i

object TexturedCube {

    private const val VOXEL = 1f / 16f

    data class PosDir(val x: Int, val y: Int, val z: Int, val nx: Int, val ny: Int, val nz: Int)

    // todo map position and normal to vertex index, and vertex index to UV
    private val posDirTo: Map<PosDir, Int> = mapOf(
        // left side
        PosDir(-1, -1, -1, -1, 0, 0) to 0,
        PosDir(-1, -1, +1, -1, 0, 0) to 1,
        PosDir(-1, +1, -1, -1, 0, 0) to 5,
        PosDir(-1, +1, +1, -1, 0, 0) to 6,

        // front side
        PosDir(-1, -1, +1, 0, 0, +1) to 1,
        PosDir(+1, -1, +1, 0, 0, +1) to 2,
        PosDir(-1, +1, +1, 0, 0, +1) to 6,
        PosDir(+1, +1, +1, 0, 0, +1) to 7,

        // right side
        PosDir(+1, -1, +1, +1, 0, 0) to 2,
        PosDir(+1, -1, -1, +1, 0, 0) to 3,
        PosDir(+1, +1, +1, +1, 0, 0) to 7,
        PosDir(+1, +1, -1, +1, 0, 0) to 8,

        // back side
        PosDir(+1, -1, -1, 0, 0, -1) to 3,
        PosDir(-1, -1, -1, 0, 0, -1) to 4,
        PosDir(+1, +1, -1, 0, 0, -1) to 8,
        PosDir(-1, +1, -1, 0, 0, -1) to 9,

        // top side
        PosDir(+1, +1, +1, 0, 1, 0) to 6,
        PosDir(-1, +1, +1, 0, 1, 0) to 7,
        PosDir(+1, +1, -1, 0, 1, 0) to 11,
        PosDir(-1, +1, -1, 0, 1, 0) to 12,

        // bottom side
        PosDir(+1, -1, +1, 0, -1, 0) to 7,
        PosDir(-1, -1, +1, 0, -1, 0) to 10,
        PosDir(+1, -1, -1, 0, -1, 0) to 12,
        PosDir(-1, -1, -1, 0, -1, 0) to 13,

        )

    fun createCuboid(
        sx: Int, sy: Int, sz: Int,
        x0: Int, y0: Int,
        texWidth: Int, texHeight: Int,
        texture: FileReference,
        scale: Float = 1f
    ): Mesh {

        check(x0 >= 0 && y0 >= 0)
        check(x0 + (sz + sx) * 2 <= texWidth)
        check(y0 + (sy + sz) <= texHeight)

        val mesh = flatCube.front.deepClone()
        val positions = mesh.positions!!
        mesh.ensureNorTanUVs()

        val newUvs = listOf(
            // bottom row
            Vector2i(0, sy + sz), // 0
            Vector2i(sz, sy + sz),
            Vector2i(sz + sx, sy + sz),
            Vector2i(sz * 2 + sx, sy + sz),
            Vector2i(sz * 2 + sx * 2, sy + sz), // 4

            // row above
            Vector2i(0, sz), // 5
            Vector2i(sz, sz),
            Vector2i(sz + sx, sz),
            Vector2i(sz * 2 + sx, sz),
            Vector2i(sz * 2 + sx * 2, sz), // 9

            // helper dot
            Vector2i(sz + sx * 2, sz), // 10

            // top row
            Vector2i(sz, 0), // 11
            Vector2i(sz + sx, 0),
            Vector2i(sz + sx * 2, 0), // 13
        )

        val normals = mesh.normals!!
        val uvs = FloatArray(positions.size / 3 * 2)
        for (i in 0 until positions.size / 3) {
            val i3 = i * 3
            val pd = PosDir(
                positions[i3].toInt(), positions[i3 + 1].toInt(), positions[i3 + 2].toInt(),
                normals[i3].toInt(), normals[i3 + 1].toInt(), normals[i3 + 2].toInt()
            )
            val i2 = i * 2
            val index = newUvs[posDirTo[pd] ?: 0]
            uvs[i2] = (index.x + x0) / texWidth.toFloat()
            uvs[i2 + 1] = 1f - (index.y + y0) / texHeight.toFloat()
        }

        val baseScale = VOXEL * 0.5f * scale
        forLoopSafely(positions.size, 3) { i ->
            positions[i] *= sx * baseScale
            positions[i + 1] *= sy * baseScale
            positions[i + 2] *= sz * baseScale
        }
        mesh.uvs = uvs
        mesh.invalidateGeometry()

        val material = Material()
        material.linearFiltering = false
        material.diffuseMap = texture
        mesh.materials = FileCacheList.of(material)
        return mesh
    }

    fun createCuboidX2(
        sx: Int, sy: Int, sz: Int,
        x0: Int, y0: Int,
        x1: Int, y1: Int,
        texWidth: Int, texHeight: Int,
        texture: FileReference,
    ): Mesh {
        val mesh0 = createCuboid(sx, sy, sz, x0, y0, texWidth, texHeight, texture, 1f)
        val mesh1 = createCuboid(sx, sy, sz, x1, y1, texWidth, texHeight, texture, (sx + 0.5f) / sx)
        return mesh0 + mesh1
    }

    operator fun Mesh.plus(other: Mesh): Mesh {
        return object : MeshJoiner<Mesh>(false, false, true) {
            override fun getMesh(element: Mesh): Mesh = element
            override fun getMaterials(element: Mesh): List<FileReference> = element.materials
        }.join(listOf(this, other))
    }
}

fun main() {
    testSceneWithUI("Player Mesh", PlayerEntity.torsoMesh)
}