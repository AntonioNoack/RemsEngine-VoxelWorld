package me.anno.remcraft.world.decorator.underground

import me.anno.remcraft.block.BlockRegistry
import me.anno.remcraft.world.Dimension
import me.anno.remcraft.world.Index.sizeX
import me.anno.remcraft.world.Index.sizeY
import me.anno.remcraft.world.Index.sizeZ
import me.anno.remcraft.world.decorator.surface.PyramidDecorator
import me.anno.remcraft.world.decorator.terrain.FlatWorldGenerator
import me.anno.utils.OS
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val n = 5
    val generator = FlatWorldGenerator(List(sizeY) { BlockRegistry.Stone })
    val dimension = Dimension(
        listOf(
            generator,
            // CaveDecorator(1e-3f, 51311L),
             RavineDecorator(0.3f * 1e-6f, 87215L),
            // todo why/how is it finding 19 as the base height??? on top another pyramid...
           /* PyramidDecorator(
                BlockRegistry.Sandstone, 15,
                BlockRegistry.Stone, 0.3f * 1e-4f, 54654L
            )*/
        )
    )
    val image = BufferedImage(n * sizeX, n * sizeZ, BufferedImage.TYPE_INT_RGB)
    for (zi in 0 until n) {
        for (xi in 0 until n) {
            val chunk = dimension.getChunk(xi, 0, zi, Int.MAX_VALUE).waitFor()!!
            for (dz in 0 until sizeZ) {
                for (dx in 0 until sizeX) {
                    val isAir = chunk.getBlock(dx, 4, dz) == BlockRegistry.Air
                    image.setRGB(
                        xi * sizeX + dx,
                        image.height - 1 - (zi * sizeZ + dz),
                        (if (dx == 0 || dz == 0) 0x111111 else 0) +
                                (if (isAir) 0 else 0x777777)
                    )
                }
            }
        }
    }
    ImageIO.write(image, "png", File(OS.desktop.getChild("caves.png").absolutePath))
}