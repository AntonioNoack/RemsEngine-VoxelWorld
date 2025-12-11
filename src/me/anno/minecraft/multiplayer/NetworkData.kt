package me.anno.minecraft.multiplayer

import me.anno.minecraft.entity.PlayerEntity
import me.anno.network.Server
import me.anno.network.TCPClient

class NetworkData {

    var server: Server? = null
    var client: TCPClient? = null

    val players = HashMap<String, PlayerEntity>()

    var lastFailed = -1_000_000_000L

}