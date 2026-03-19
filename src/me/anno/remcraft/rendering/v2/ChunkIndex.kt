package me.anno.remcraft.rendering.v2

import org.joml.Vector3i

object ChunkIndex {
    fun encodeChunkIndex(xi: Int, yi: Int, zi: Int): Long {
        val xl = xi.toLong() and 0xffffff
        val zl = zi.toLong() and 0xffffff
        val yl = yi.toLong() and 0xffff
        return xl + zl.shl(24) + yl.shl(48)
    }

    fun decodeChunkIndex(encoded: Long, dst: Vector3i): Vector3i {
        return dst.set(
            decodeX(encoded),
            decodeY(encoded),
            decodeZ(encoded),
        )
    }

    private fun decodeX(encoded: Long): Int {
        return encoded.shl(8).toInt().shr(8)
    }

    private fun decodeZ(encoded: Long): Int {
        return encoded.shr(16).toInt().shr(8)
    }

    private fun decodeY(encoded: Long): Int {
        return encoded.shr(32).toInt().shr(16)
    }
}