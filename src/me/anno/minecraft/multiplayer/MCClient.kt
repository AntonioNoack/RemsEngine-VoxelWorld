package me.anno.minecraft.multiplayer

import me.anno.minecraft.entity.Player
import me.anno.network.Protocol
import me.anno.network.TCPClient
import java.net.Socket

class MCClient : TCPClient {

    val primary: Player

    val network: NetworkData

    constructor(primary: Player, socket: Socket, protocol: Protocol, name: String) : super(socket, protocol, name) {
        this.primary = primary
        this.network = this.primary.networkData
    }

    constructor(primary: Player, socket: Socket, protocol: Protocol, randomId: Int) : super(socket, protocol, randomId) {
        this.primary = primary
        this.network = this.primary.networkData
    }

}