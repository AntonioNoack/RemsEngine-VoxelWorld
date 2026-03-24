package me.anno.remcraft.utils

import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Color.a
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Ints.isPowerOf2
import org.joml.Vector2i
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

fun main() {
    // todo list files and candidates for joining
    // todo join them if total <= 4096²
    // todo delete originals after a check
    val source = File("/media/antonio/4TB WDRed/Assets/humbleIcons")
    process(source)
}

fun Int.ceilToPowerOf2(): Int {
    if (isPowerOf2()) return this
    val numBits = countLeadingZeroBits()
    return 1 shl (32 - numBits)
}

fun process(source: File) {
    val children = source.listFiles()!!

    // collapse duplicate names
    if (children.size == 1 && children[0].isDirectory) {
        val inner = children[0]
        for (file in inner.listFiles()!!) {
            val target = File(source, file.name)
            file.renameTo(target)
        }
        inner.delete()
        process(source)
        return
    }

    val imageGroups = children
        .filter { it.extension.lowercase() == "png" && !it.name.startsWith("Textures-") }
        .sortedBy { it.name.lowercase() }
        .map { it to ImageIO.read(it) }
        .groupBy { (_, img) -> Vector2i(img.width, img.height) }

    for ((size, images) in imageGroups) {
        if (images.size < 5) continue
        check((size.x == 256 && abs(size.y - 256) < 4) || (size.x == 512 && size.y == 512)) {
            "Sizes: $size in $source"
        }

        var countX = ceil(sqrt(images.size.toFloat())).toInt()
        countX = countX.ceilToPowerOf2()

        var countY = ceilDiv(images.size, countX)
        countY = countY.ceilToPowerOf2()

        val hasAlpha = images.any2 { (_, image) ->
            (0 until size.y).any { y ->
                (0 until size.x).any { x ->
                    image.getRGB(x, y).a() < 120
                }
            }
        }

        val sizes = listOf(16, 32, 48, 64, 96, 128, 256, 512)
            .filter { it <= size.x }
        for (size in sizes) {
            val result = BufferedImage(countX * size, countY * size, if (hasAlpha) 2 else 1)
            val gfx = result.graphics
            for (i in images.indices) {
                val srcImage = images[i].second
                val x0 = (i % countX) * size
                val y0 = (i / countX) * size
                gfx.drawImage(srcImage, x0, y0, size, size, null)
            }
            gfx.dispose()
            ImageIO.write(result, "png", File(source, "Textures-$size.png"))
        }

        println("$source, $size, ${images.size} -> $countX x $countY")
        images.forEach { it.first.delete() }
    }

    // unpacking Zips
    for (child in children.filter { it.extension.lowercase() == "zip" }) {
        val dst = File(child.parentFile, child.nameWithoutExtension)
        if (!dst.exists()) {
            ZipInputStream(FileInputStream(child)).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val dstI = File(dst, entry.name)
                    dstI.parentFile.mkdirs()
                    dstI.writeBytes(zis.readBytes())
                }
            }
            child.delete()
            process(dst)
        } else println("$dst already exists, cannot unzip")
    }

    // processing child directories
    for (child in children.filter { it.isDirectory }) {
        process(child)
    }
}