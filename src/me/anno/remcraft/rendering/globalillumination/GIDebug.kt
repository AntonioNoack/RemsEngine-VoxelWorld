package me.anno.remcraft.rendering.globalillumination

import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.block.BlockType
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeSide
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeZ
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosZ
import me.anno.remcraft.world.Dimension
import me.anno.remcraft.world.SampleDimensions
import me.anno.utils.OS.res
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import org.joml.Vector3f

// implement a prototype on the CPU-side for now
fun main() {

    OfficialExtensions.initForTests()

    val simpleWorld = false
    val dimension =
        if (simpleWorld) Dimension(emptyList())
        else SampleDimensions.perlin2dDim
    val gi = GlobalIllumination(dimension)

    if (simpleWorld) {
        fun set(x: Int, y: Int, z: Int, type: BlockType) {
            dimension.getChunkAt(x, y, z)!!
                .setBlockQuickly(x, y, z, type.id)
        }

        fun fill(x: Int, y: Int, z: Int, sx: Int, sy: Int, sz: Int, type: BlockType) {
            for (dx in 0 until sx) {
                for (dy in 0 until sy) {
                    for (dz in 0 until sz) {
                        set(x + dx, y + dy, z + dz, type)
                    }
                }
            }
        }

        fill(-5, 0, -5, 11, 1, 11, BlockRegistry.Stone)
        fill(-3, 3, -3, 6, 1, 6, BlockRegistry.Stone)

        // set(1, 1, 1, BlockRegistry.Stone)
    }

    val dx = 1
    val dz = 1
    val y0 = if (simpleWorld) 0 else 1
    val y1 = if (simpleWorld) 0 else 2

    for (xi in -dx..dx) {
        for (zi in -dz..dz) {
            for (yi in y0..y1) {
                val chunk = dimension.getChunk(xi, yi, zi, Int.MAX_VALUE)
                gi.addChunk(chunk.waitFor()!!)
            }
        }
    }

    println("UniqueFaces: ${gi.faces.size}, Connections: ${gi.connections.totalSize()}")

    val defaultSky = Skybox()
    val sunDir = Vector3f(defaultSky.sunBaseDir)
        .rotate(defaultSky.sunRotation)
        .normalize()
    val sunColors = BlockSide.entries.map { side ->
        val brightness = 5f * sunDir.dot(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
        if (brightness > 0f) {
            Vector3f(1f, 1f, 0.9f).mul(brightness)
        } else null
    }

    val skyColors = BlockSide.entries.map { _ ->
        Vector3f(0.8f, 0.9f, 0.92f).mul(0.5f)
    }

    val light = gi.lightTransport(
        sunDir, sunColors,
        skyColors, 20
    )

    testSceneWithUI("GIMesh", createDebugMesh(gi, light.values))
}

fun createDebugMesh(gi: GlobalIllumination, light: FloatArray): Mesh {
    // render chunk
    val mesh = Mesh()
    val positions = FloatArrayList()
    val normals = FloatArrayList()
    val colors = FloatArrayList()
    val uvs = FloatArrayList()

    // todo this already works pretty well...
    //  -> special shader to map color into emissive
    //  and to load/show proper texture

    val flipSides = BooleanArray(6)
    flipSides[BlockSide.NX.ordinal] = true
    flipSides[BlockSide.NY.ordinal] = true
    flipSides[BlockSide.NZ.ordinal] = true

    gi.faces.forEach { hash, faceId ->
        val x = decodeX(hash)
        val y = decodeY(hash)
        val z = decodeZ(hash)
        val side = decodeSide(hash)

        val cr = light[faceId * 3 + 0]
        val cg = light[faceId * 3 + 1]
        val cb = light[faceId * 3 + 2]

        val block = gi.dimension.getBlockAt(x, y, z) ?: BlockRegistry.Stone

        val texU = block.texId.and(15)
        val texV = block.texId.shr(4)

        fun add(du: Float, dv: Float) {
            val px = getPosX(x, side, du, dv).toFloat()
            val py = getPosY(y, side, du, dv).toFloat()
            val pz = getPosZ(z, side, du, dv).toFloat()
            positions.add(px, py, pz)
            normals.add(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
            colors.add(cr, cg, cb)
            val u = du + 0.5f
            val v = dv + 0.5f
            uvs.add((u + texU) / 16f, 1f - (v + texV) / 32f)
        }

        if (flipSides[side.ordinal]) {
            add(-0.5f, -0.5f)
            add(+0.5f, +0.5f)
            add(-0.5f, +0.5f)

            add(-0.5f, -0.5f)
            add(+0.5f, -0.5f)
            add(+0.5f, +0.5f)
        } else {
            add(-0.5f, -0.5f)
            add(-0.5f, +0.5f)
            add(+0.5f, +0.5f)

            add(-0.5f, -0.5f)
            add(+0.5f, +0.5f)
            add(+0.5f, -0.5f)
        }
    }

    mesh.positions = positions.toFloatArray()
    mesh.normals = normals.toFloatArray()
    mesh.lightLevels = colors.toFloatArray()
    mesh.uvs = uvs.toFloatArray()

    mesh.materials = listOf(Material().apply {
        shader = GITextureShader
        linearFiltering = false
        diffuseMap = res.getChild("textures/blocks/Blocks.png")
    }.ref)

    return mesh
}

var Mesh.lightLevels: FloatArray?
    get() = getAttr("lightLevels", FloatArray::class)
    set(value) = setAttr("lightLevels", value, lightLevelType)

private val lightLevelType = Attribute("lightLevels", AttributeType.FLOAT, 3)