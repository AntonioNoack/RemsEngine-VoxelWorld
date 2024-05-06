package me.anno.minecraft.multiplayer

import me.anno.minecraft.entity.Player
import me.anno.network.Protocol
import me.anno.network.TCPClient
import java.net.Socket

class MCClient(
    socket: Socket, protocol: Protocol, randomId: Int,
    val primary: Player, val network: NetworkData
) : TCPClient(socket, protocol, randomId) {

    constructor(primary: Player, socket: Socket, protocol: Protocol, name: String) :
            this(socket, protocol, 0, primary, primary.networkData) {
        this.name = name
        this.uuid = name
    }

    constructor(primary: Player, socket: Socket, protocol: Protocol, randomId: Int) :
            this(socket, protocol, randomId, primary, primary.networkData)

}