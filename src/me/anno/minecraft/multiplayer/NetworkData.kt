package me.anno.minecraft.multiplayer

import me.anno.minecraft.entity.Player
import me.anno.network.Server
import me.anno.network.TCPClient

class NetworkData {

    var server: Server? = null
    var client: TCPClient? = null

    val players = HashMap<String, Player>()

    var lastFailed = (-1e9).toLong()

}