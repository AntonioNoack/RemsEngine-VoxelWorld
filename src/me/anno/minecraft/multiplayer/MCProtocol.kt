package me.anno.minecraft.multiplayer

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.minecraft.entity.Player
import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.POS1Packet
import me.anno.network.packets.PingPacket
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import java.io.DataInputStream
import java.io.IOException
import java.net.BindException
import java.net.Socket
import kotlin.math.abs
import kotlin.random.Random

object MCProtocol {

    private val protocol = Protocol("REMC", NetworkProtocol.TCP)

    class POSXPacket : POS1Packet("POSX") {
        override fun receiveData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
            super.receiveData(server, client, dis, size)
            println("${client.name}: $x $y $z")
            if (client.name != name) {
                players[client.name]?.apply {
                    val entity = entity!!
                    entity.transform.localPosition = Vector3d(x, y, z)
                }
            }
        }
    }

    init {
        protocol.register(PingPacket())
        protocol.register(POSXPacket())
    }

    private val port = 65025
    private var server: Server? = null
    private var client: TCPClient? = null
    private val players = HashMap<String, Player>()
    private var name = "Gustav${Random(System.nanoTime()).nextInt(1000)}"
    private var player = Player()

    var lastFailed = 0L

    fun updatePlayers(entities: Entity) {
        // doesn't work yet
        // todo make multiplayer work
        return
        if (abs(Engine.gameTime - lastFailed) < 1e9) return
        try {
            if (server == null && (client == null || client!!.isClosed)) {
                if (!tryStartServer()) {
                    tryStartClient()
                }
            }
            if (player.entity == null) {
                createPlayer(name, entities, player)
            }
            val client = client
            val server = server
            when {
                client != null && client.socket.isBound && client.socket.isConnected -> {
                    client.send(null, createPacket(player))
                    client.dos.flush()
                }
                server != null -> {
                    server.broadcast(createPacket(player))
                    for (player2 in server.clients) {
                        val name = player2.name
                        val player2i = players.getOrPut(name) { createPlayer(name, entities) }
                        server.broadcast(createPacket(player2i))
                    }
                    players.removeIf { p ->
                        if (server.clients.none { it.name == p.key }) {
                            entities.remove(p.value.entity!!)
                            true
                        } else false
                    }
                    for (client2 in server.clients) {
                        client2.dos.flush()
                    }
                }
            }
        } catch (e: IOException) {
            lastFailed = Engine.gameTime
            e.printStackTrace()
        }
    }

    fun stop() {
        server?.close()
        client?.close()
        server = null
        client = null
        LOGGER.info("closed")
    }

    private fun createPlayer(name: String, entities: Entity, player: Player = Player()): Player {
        val child = Entity()
        child.name = name
        player.name = name
        child.add(player)
        entities.add(child)
        return player
    }

    private fun createPacket(player: Player): POSXPacket {
        val entity = player.entity!!
        val pos = entity.transform.globalPosition
        val packet = POSXPacket()
        packet.x = pos.x
        packet.y = pos.y
        packet.z = pos.z
        return packet
    }

    private fun tryStartClient() {
        client = try {
            LOGGER.info("starting client")
            val socket = Socket("localhost", port)
            val client = TCPClient(socket, name)
            client.startAsync(protocol)
            client
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tryStartServer(): Boolean {
        return try {
            val server = Server()
            server.register(protocol)
            server.start(port, 0)
            LOGGER.info("started server")
            this.server = server
            true
        } catch (e: BindException) {
            LOGGER.info("server port is already bound")
            this.server = null
            false
        } catch (e: Exception) {
            e.printStackTrace()
            this.server = null
            false
        }
    }

    private val LOGGER = LogManager.getLogger(MCProtocol::class)

}