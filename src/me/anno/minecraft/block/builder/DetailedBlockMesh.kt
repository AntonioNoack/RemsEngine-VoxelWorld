package me.anno.minecraft.block.builder

import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.minecraft.rendering.v2.TextureMaterial
import me.anno.minecraft.rendering.v2.VertexFormat.detailsBlockVertexData
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.AABBd
import org.joml.AABBf
import java.util.*

abstract class DetailedBlockMesh<DataArray> : PrefabSaveable(), IMesh {

    companion object {
        const val DETAIL_SIZE = 16
        const val SCALE = 1f / DETAIL_SIZE
        val pseudoComponent = MeshComponent()
        val materialsI = FileCacheList.of(TextureMaterial.solid)

        fun getDetailIndex(x: Int, y: Int, z: Int): Int {
            return if (x in 0 until DETAIL_SIZE && y in 0 until DETAIL_SIZE && z in 0 until DETAIL_SIZE) {
                x + y.shl(4) + z.shl(8)
            } else -1
        }
    }

    // x,y,z,texId
    var data: DataArray? = null

    var validBounds = false
    val calcBounds = AABBf()

    var buffer: StaticBuffer? = null

    val voxels = BitSet(DETAIL_SIZE * DETAIL_SIZE * DETAIL_SIZE)
    fun getVoxel(x: Int, y: Int, z: Int): Boolean {
        val index = getDetailIndex(x, y, z)
        return index >= 0 && voxels[index]
    }

    override var materials: List<FileReference> = materialsI

    abstract fun union(data: DataArray, i: Int, dst: AABBf)
    abstract fun getDataSize(data: DataArray): Int
    abstract fun toMesh(): Mesh

    fun calculateBounds() {
        val data = data ?: return
        val bounds = calcBounds
        bounds.clear()
        forLoopSafely(getDataSize(data), 4) { i ->
            union(data, i, bounds)
        }
        validBounds = true
    }

    override val numPrimitives: Long
        get() {
            val data = data ?: return 0
            return (getDataSize(data) shr 2).toLong()
        }

    override fun getBounds(): AABBf {
        if (!validBounds) calculateBounds()
        return calcBounds
    }

    override fun getGlobalBounds(): AABBd? {
        return AABBd().set(getBounds())
    }

    override fun draw(
        pipeline: Pipeline?,
        shader: Shader,
        materialIndex: Int,
        drawLines: Boolean
    ) {
        ensureBuffer()
        val buffer = buffer ?: return
        buffer.draw(shader, DrawMode.TRIANGLES)
    }

    override fun drawInstanced(
        pipeline: Pipeline,
        shader: Shader,
        materialIndex: Int,
        instanceData: Buffer,
        drawLines: Boolean
    ) {
        ensureBuffer()
        val buffer = buffer ?: return
        buffer.drawInstanced(shader, instanceData, DrawMode.TRIANGLES)
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        pipeline.addMesh(this, pseudoComponent, transform)
    }

    override val vertexData: MeshVertexData
        get() = detailsBlockVertexData

}