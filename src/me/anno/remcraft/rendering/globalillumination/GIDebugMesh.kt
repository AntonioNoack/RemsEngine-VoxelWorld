package me.anno.remcraft.rendering.globalillumination

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.maths.Packing.unpackHighFrom32
import me.anno.maths.Packing.unpackLowFrom32
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeSide
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.decodeZ
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.encodeSide
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosX
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosY
import me.anno.remcraft.rendering.globalillumination.GlobalIllumination.Companion.getPosZ
import me.anno.remcraft.rendering.globalillumination.gpu.sign
import me.anno.remcraft.world.Dimension
import me.anno.utils.OS.res
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3f
import org.joml.Vector3i
import speiger.primitivecollections.LongToIntHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun createDebugMesh(
    dimension: Dimension,
    faces: IntArrayList,
    light: IntArray,
    interpolateColors: Boolean,
): Mesh {
    val lightScale = 5f / light.max()
    return createDebugMesh(dimension, interpolateColors) { callback ->
        for (i in faces.indices step 4) {
            val faceId = i shr 2
            val fx = faces[i]
            val fy = faces[i + 1]
            val x = unpackLowFrom32(fx, true)
            val y = unpackHighFrom32(fx, true)
            val z = unpackLowFrom32(fy, true)
            val sideId = unpackHighFrom32(fy, false)
            val side = BlockSide.entries[sideId]

            val cr = light[faceId * 4 + 0] * lightScale
            val cg = light[faceId * 4 + 1] * lightScale
            val cb = light[faceId * 4 + 2] * lightScale
            callback(x, y, z, side, cr, cg, cb)
        }
    }
}

fun createDebugMesh(
    gi: GlobalIllumination,
    light: FloatArray,
    interpolateColors: Boolean,
): Mesh {
    return createDebugMesh(gi.dimension, interpolateColors) { callback ->
        gi.faces.forEach { hash, faceId ->
            val x = decodeX(hash)
            val y = decodeY(hash)
            val z = decodeZ(hash)
            val side = decodeSide(hash)

            val cr = light[faceId * 3 + 0]
            val cg = light[faceId * 3 + 1]
            val cb = light[faceId * 3 + 2]
            callback(x, y, z, side, cr, cg, cb)
        }
    }
}

val cube8 = arrayOf(
    Vector3i(0, 0, 0),
    Vector3i(0, 0, 1),
    Vector3i(0, 1, 0),
    Vector3i(0, 1, 1),
    Vector3i(1, 0, 0),
    Vector3i(1, 0, 1),
    Vector3i(1, 1, 0),
    Vector3i(1, 1, 1),
)

fun createDebugMesh(
    dimension: Dimension,
    interpolateColors: Boolean,
    iterateFaces: (
        callback: (
            x: Int, y: Int, z: Int, side: BlockSide,
            r: Float, g: Float, b: Float,
        ) -> Unit
    ) -> Unit
): Mesh {
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

    if (interpolateColors) {
        val faces = LongToIntHashMap(-1, 1 shl 10)
        val faceColors = FloatArrayList()
        iterateFaces { x, y, z, side, cr, cg, cb ->
            faces.put(encodeSide(x, y, z, side), faceColors.size)
            faceColors.add(cr, cg, cb)
        }

        val color0 = Vector3f()
        val color00 = Vector3f()
        val color01 = Vector3f()
        val color10 = Vector3f()
        val color11 = Vector3f()

        val indices = IntArrayList()
        var k = 0

        faces.forEach { hash, colorOffset ->
            val x = decodeX(hash)
            val y = decodeY(hash)
            val z = decodeZ(hash)
            val side = decodeSide(hash)

            val cr = faceColors[colorOffset]
            val cg = faceColors[colorOffset + 1]
            val cb = faceColors[colorOffset + 2]
            color0.set(cr, cg, cb)

            val block = dimension.getBlockAt(x, y, z) ?: BlockRegistry.Stone

            val texU = block.texId.and(15)
            val texV = block.texId.shr(4)

            fun addBaseData(du: Float, dv: Float) {
                val px = getPosX(x, side, du, dv).toFloat()
                val py = getPosY(y, side, du, dv).toFloat()
                val pz = getPosZ(z, side, du, dv).toFloat()
                positions.add(px, py, pz)
                normals.add(side.x.toFloat(), side.y.toFloat(), side.z.toFloat())
                val u = du + 0.5f
                val v = dv + 0.5f
                uvs.add((u + texU) / 16f, 1f - (v + texV) / 32f)
            }

            fun getEdgeColor(du: Float, dv: Float, dst: Vector3f) {

                val dx = min(sign(getPosX(x, side, du, dv) - getPosX(x, side, 0f, 0f)).toInt() + side.x, 0)
                val dy = min(sign(getPosY(y, side, du, dv) - getPosY(y, side, 0f, 0f)).toInt() + side.y, 0)
                val dz = min(sign(getPosZ(z, side, du, dv) - getPosZ(z, side, 0f, 0f)).toInt() + side.z, 0)

                var cr = 0f
                var cg = 0f
                var cb = 0f
                var visibleBlocks = 0
                var weight = 0

                var ctr = 0
                for (cube in cube8) {
                    val xi = x + dx + cube.x
                    val yi = y + dy + cube.y
                    val zi = z + dz + cube.z
                    val isInFront = (xi - x) * side.x + (yi - y) * side.y + (zi - z) * side.z
                    check(isInFront == 0 || isInFront == 1)
                    if (isInFront == 0) {
                        val colorOffsetI = faces[encodeSide(xi, yi, zi, side)]
                        if (colorOffsetI != -1) {
                            // averaging illumination
                            cr += faceColors[colorOffsetI]
                            cg += faceColors[colorOffsetI + 1]
                            cb += faceColors[colorOffsetI + 2]
                            weight++
                        }
                        ctr++
                    } else {
                        // blocking light on edges
                        val block = dimension.getBlockAt(xi, yi, zi)!!
                        if (!block.isSolid) visibleBlocks++
                    }
                }
                check(ctr == 4)

                // count how many blocks are blocking our view -> ambient occlusion
                // average light from corner of all non-blocking blocks -> smoother lighting
                // todo somehow subtract sunlight#0, and add that back using direct lighting & shadow map (or per-pixel RT)

                // how can visibleBlocks be 0???
                // why is there copper ore in the simple world???
                //  -> todo it's using the wrong dimension

                val factor = visibleBlocks / (weight * 4f)
                dst.set(cr * factor, cg * factor, cb * factor)
            }

            fun add(du: Float, dv: Float, color: Vector3f) {
                addBaseData(du, dv)
                colors.add(color)
            }

            val du = 0.5f
            val flip = flipSides[side.ordinal]
            val dv = if (flip) -0.5f else +0.5f

            getEdgeColor(-du, -dv, color00)
            getEdgeColor(-du, +dv, color01)
            getEdgeColor(+du, -dv, color10)
            getEdgeColor(+du, +dv, color11)

            if (false) {
                // simple cross pattern
                add(-du, -dv, color00)
                add(-du, +dv, color01)
                add(0f, 0f, color0)

                add(-du, +dv, color01)
                add(+du, +dv, color11)
                add(0f, 0f, color0)

                add(+du, +dv, color11)
                add(+du, -dv, color10)
                add(0f, 0f, color0)

                add(+du, -dv, color10)
                add(-du, -dv, color00)
                add(0f, 0f, color0)
            } else {
                // diagonal center doesn't look good for edges,
                //  but straight looks even weirder
                val f = 0.25f

                add(-du, -dv, color00)
                add(-du, +dv, color01)
                add(+du, -dv, color10)
                add(+du, +dv, color11)

                if (false) {
                    // straight center
                    val g = if (flip) +f else -f
                    add(-f, +g, color0)
                    add(+f, -g, color0)
                    add(+f, +g, color0)
                    add(-f, -g, color0)
                } else {
                    // diagonal center
                    val g = if (flip) +f else -f
                    add(-f, 0f, color0)
                    add(+f, 0f, color0)
                    add(0f, +g, color0)
                    add(0f, -g, color0)
                }

                indices.add(k + 0, k + 1, k + 4)
                indices.add(k + 1, k + 3, k + 7)
                indices.add(k + 3, k + 2, k + 5)
                indices.add(k + 2, k + 0, k + 6)

                indices.add(k + 0, k + 4, k + 6)
                indices.add(k + 1, k + 7, k + 4)
                indices.add(k + 3, k + 5, k + 7)
                indices.add(k + 2, k + 6, k + 5)

                indices.add(k + 4, k + 7, k + 6)
                indices.add(k + 6, k + 7, k + 5)

                // added 8 vertices
                k += 8

            }
        }
        mesh.indices = indices.toIntArray()
    } else {
        iterateFaces { x, y, z, side, cr, cg, cb ->

            val block = dimension.getBlockAt(x, y, z) ?: BlockRegistry.Stone

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

            val du = 0.5f
            val dv = if (flipSides[side.ordinal]) -0.5f else +0.5f

            add(-du, -dv)
            add(-du, +dv)
            add(+du, +dv)

            add(-du, -dv)
            add(+du, +dv)
            add(+du, -dv)
        }
    }

    if (false) {
        // GI must be calculated in linear space
        // -> convert GI to sRGB
        val colors1 = colors.values
        for (i in 0 until colors.size) {
            colors1[i] = sqrt(max(colors1[i], 0f))
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