package me.anno.minecraft.multiplayer

import me.anno.minecraft.entity.Player
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.studio.StudioBase.Companion.addEvent
import org.apache.logging.log4j.LogManager
import java.io.DataInputStream
import java.io.DataOutputStream

class PlayerStatePacket(magic: String = "JOIN") : Packet(magic) {

    constructor(name: String, join: Boolean) : this() {
        this.name = name
        this.join = join
    }

    var join = false
    var name = ""

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeBoolean(join)
        dos.writeUTF(name)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        join = dis.readBoolean()
        name = dis.readUTF()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        super.onReceive(server, client)
        if (server == null) {
            client as MCClient
            // save all variables, because this packet instance will be reused
            val name = name
            val join = join
            LOGGER.info("[${client.primary.name}] $name ${if (join) "joint" else "left"} the game | ${System.identityHashCode(client.network.players)}")
            addEvent {
                val players = client.network.players
                synchronized(players) {
                    if (join) {
                        players.getOrPut(name) { Player(false, name) }
                        LOGGER.info("Added $name")
                    } else {
                        players.remove(name)?.entity?.removeFromParent()
                        LOGGER.info("Removed $name")
                    }
                }
            }
        }
    }

    override val className: String = "PlayerStatePacket"

    companion object {
        private val LOGGER = LogManager.getLogger(PlayerStatePacket::class)
    }

}