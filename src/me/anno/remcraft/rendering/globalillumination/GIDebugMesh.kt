package me.anno.remcraft.rendering.globalillumination

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeSide
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeZ
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosZ
import me.anno.utils.OS.res
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add


fun createDebugMesh(gi: GlobalIllumination, light: FloatArray): Mesh {
    // render chunk
    val mesh = Mesh()
    val positions = FloatArrayList()
    val normals = FloatArrayList()
    val colors = FloatArrayList()
    val uvs = FloatArrayList()

    // this already works pretty well...
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

            // todo count how many blocks are blocking our view -> ambient occlusion
            // todo average light from corner of all non-blocking blocks -> smoother lighting
            // todo somehow subtract sunlight#0, and add that back using direct lighting & shadow map (or per-pixel RT)

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