package me.anno.minecraft.rendering.v2

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.minecraft.block.builder.DetailedBlockMesh32
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes2
import me.anno.minecraft.rendering.v2.VertexFormat.detailsBlockVertexData
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3i

class DetailChunkRenderer(val material: Material) :
    UniqueMeshRenderer<DetailedBlockMesh32, Vector3i>(blockAttributes2, detailsBlockVertexData, DrawMode.TRIANGLES) {

    override val hasVertexColors: Int get() = 1
    override val materials: List<FileReference> = listOf(material.ref)

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        dstUnion.all()
        localAABB.all()
        globalAABB.all()
        return true
    }

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
     * */
    override fun getTransformAndMaterial(key: Vector3i, transform: Transform): Material = material

    override fun getData(key: Vector3i, mesh: DetailedBlockMesh32): StaticBuffer? {
        if (mesh.numPrimitives == 0L) return null
        mesh.ensureBuffer()
        return mesh.buffer
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val component = DetailChunkRenderer(TextureMaterial.solid)
            testSceneWithUI("DetailChunkRenderer", component)
        }
    }
}
