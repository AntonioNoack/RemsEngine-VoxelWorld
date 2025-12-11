package me.anno.minecraft.multiplayer

import me.anno.minecraft.entity.PlayerEntity
import me.anno.network.Protocol
import me.anno.network.TCPClient
import java.net.Socket

class MCClient(
    socket: Socket, protocol: Protocol, randomId: Int,
    val primary: PlayerEntity, val network: NetworkData
) : TCPClient(socket, protocol, randomId) {

    constructor(primary: PlayerEntity, socket: Socket, protocol: Protocol, name: String) :
            this(socket, protocol, 0, primary, primary.networkData) {
        this.name = name
        this.uuid = name
    }

    constructor(primary: PlayerEntity, socket: Socket, protocol: Protocol, randomId: Int) :
            this(socket, protocol, randomId, primary, primary.networkData)

}