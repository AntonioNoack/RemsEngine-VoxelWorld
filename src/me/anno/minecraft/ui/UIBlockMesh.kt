package me.anno.minecraft.ui

import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBufferUtils.replaceBuffer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.minecraft.rendering.v2.ChunkRenderer.Companion.putVertex
import me.anno.minecraft.rendering.v2.VertexFormat.blockAttributes
import me.anno.minecraft.rendering.v2.VertexFormat.blockVertexData

class UIBlockMesh(val blockIndex: Int) : Mesh() {

    override val vertexData: MeshVertexData
        get() = blockVertexData

    override fun createMeshBuffer() {
        needsMeshUpdate = false

        // not the safest, but well...
        val pos = positions ?: return
        if (pos.isEmpty()) return

        val vertexCount = pos.size / 3
        val indices = indices

        hasBonesInBuffer = false
        hasVertexColors = 0

        val name = "Block#$blockIndex"
        val buffer = replaceBuffer(name, blockAttributes, vertexCount, buffer)
        buffer.drawMode = drawMode
        this.buffer = buffer

        triBuffer = replaceBuffer(buffer, indices, triBuffer)
        triBuffer?.drawMode = drawMode

        val bytes = buffer.nioBuffer!!
        for (i in 0 until vertexCount) {
            // upload all data of one vertex
            val i3 = i * 3
            putVertex(bytes, pos[i3], pos[i3 + 1], pos[i3 + 2], blockIndex)
        }

        updateHelperMeshes()
    }
}