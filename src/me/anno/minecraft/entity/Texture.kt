package me.anno.minecraft.entity

import me.anno.cache.FileCacheList
import me.anno.ecs.components.mesh.material.Material
import me.anno.io.files.FileReference

class Texture(val src: FileReference) {
    val material = Material().apply {
        diffuseMap = src
        linearFiltering = false
    }
    val materials = FileCacheList.of(material)
}