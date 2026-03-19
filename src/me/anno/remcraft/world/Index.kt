package me.anno.remcraft.world

object Index {

    const val bitsX = 5
    const val bitsY = 5
    const val bitsZ = 5

    const val sizeX = 1 shl bitsX
    const val sizeY = 1 shl bitsY
    const val sizeZ = 1 shl bitsZ

    const val totalSize = sizeX * sizeY * sizeZ

    const val maskX = sizeX - 1
    const val maskY = sizeY - 1
    const val maskZ = sizeZ - 1

    fun getIndex(localX: Int, localY: Int, localZ: Int): Int {
        return getUnsafeIndex(
            localX and maskX,
            localY and maskY,
            localZ and maskZ
        )
    }

    fun getUnsafeIndex(localX: Int, localY: Int, localZ: Int): Int {
        return localX or (localZ or localY.shl(bitsZ)).shl(bitsX)
    }

    fun indexToX(index: Int): Int {
        return index.and(maskX)
    }

    fun indexToY(index: Int): Int {
        return (index.shr(bitsX + bitsZ)).and(maskY)
    }

    fun indexToZ(index: Int): Int {
        return index.shr(bitsX).and(maskZ)
    }
}