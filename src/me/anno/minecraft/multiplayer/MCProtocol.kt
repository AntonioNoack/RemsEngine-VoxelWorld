package me.anno.minecraft.multiplayer

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.RenderView
import me.anno.minecraft.entity.Player
import me.anno.minecraft.multiplayer.POSXPacket.Companion.createPacket
import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.PingPacket
import me.anno.utils.OS.documents
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.BindException
import java.net.InetAddress
import java.net.Socket
import kotlin.math.abs

object MCProtocol {

    private val tcpProtocol = Protocol("REMT", NetworkProtocol.TCP)
    private val udpProtocol = Protocol("REMU", NetworkProtocol.UDP)

    init {
        tcpProtocol.register(PingPacket())
        tcpProtocol.register(POSXPacket())
        tcpProtocol.register(PlayerStatePacket())
        udpProtocol.register(PingPacket())
        udpProtocol.register(POSXPacket())
    }

    private val tcpPort = 65024
    private val udpPort = tcpPort + 1

    fun updatePlayers(player: Player, entities: Entity) {

        // Packet.debugPackets = true
        // doesn't work yet
        // todo make multiplayer work

        // timeout after failure
        val data = player.networkData
        if (abs(Time.gameTimeN - data.lastFailed) < 1e9) return

        data.players.getOrPut(player.name) { createPlayer(player.name, entities, player) }

        try {

            if (data.client?.isClosed != false) {
                tryStartServer(player)
                tryStartClient(player)
            }

            for ((name, player2) in data.players) {
                val entity = player2.entity
                if (entity == null || entity.parent != entities) {
                    LOGGER.info("creating player $name")
                    createPlayer(name, entities, player2)
                }
            }

            val transform = player.transform
            val renderView = RenderView.currentInstance
            if (transform != null && renderView != null) {
                val pos = renderView.orbitCenter
                transform.setLocal(renderView.editorCameraNode.transform.localTransform)
                transform.localPosition = transform.localPosition.set(pos.x, pos.y, pos.z)
                transform.smoothUpdate()
            }

            val client = data.client
            if (client != null && client.socket.isBound && client.socket.isConnected) {
                if (player.entity != null) {
                    client.sendUDP(createPacket(player, client), udpProtocol, false)
                    client.dos.flush()
                }
            }

            val server = data.server
            if (server != null) {
                server.forAllClientsSync { client2 ->
                    val name = client2.name
                    val player2i = synchronized(data.players) { data.players[name] }
                    if (player2i != null) {
                        server.broadcast(createPacket(player2i, client2))
                    }
                }
                server.forAllClientsSync { client2 ->
                    client2.dos.flush()
                }
            }

        } catch (e: IOException) {
            data.lastFailed = Time.gameTimeN
            e.printStackTrace()
            stop(data)
        }
    }

    fun stop(player: Player) {
        stop(player.networkData)
    }

    fun stop(data: NetworkData) {
        data.server?.close()
        data.client?.close()
        data.server = null
        data.client = null
        LOGGER.info("closed")
        for (player in data.players.values) {
            player.entity?.removeFromParent()
        }
        data.players.clear()
    }

    fun createPlayer(name: String, entities: Entity, player: Player = Player()): Player {
        val entity = Entity(name)
        player.name = name
        entity.add(player)
        entity.add(MeshComponent(documents.getChild("redMonkey.glb")))
        entities.add(entity)
        return player
    }

    private fun tryStartClient(player: Player) {
        player.networkData.client = try {
            LOGGER.info("starting client")
            val socket = TCPClient.createSocket(InetAddress.getLocalHost(), tcpPort, tcpProtocol)
            val client = MCClient(player, socket, tcpProtocol, player.name)
            client.udpPort = udpPort
            client.startClientSideAsync()
            client
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tryStartServer(player: Player): Boolean {
        return try {
            val server = object : Server() {

                override fun createClient(clientSocket: Socket, protocol: Protocol, randomId: Int): TCPClient {
                    return MCClient(player, clientSocket, protocol, randomId)
                }

                override fun onClientConnected(client: TCPClient) {
                    super.onClientConnected(client)
                    LOGGER.info("[Server] ${client.name} joined the game")
                    // send info about new player to everyone else
                    broadcast(PlayerStatePacket(client.name, true))
                    // send new player all info about everyone else
                    forAllClients { client2 ->
                        if (client2 !== client) {
                            client.sendTCP(PlayerStatePacket(client2.name, true))
                        }
                    }
                    client.flush()
                }

                override fun onClientDisconnected(client: TCPClient) {
                    super.onClientDisconnected(client)
                    LOGGER.info("[Server] ${client.name} left the game")
                    broadcast(PlayerStatePacket(client.name, false))
                }
            }
            server.register(tcpProtocol)
            server.register(udpProtocol)
            server.start(tcpPort, udpPort)
            LOGGER.info("started server, assigning to player ${player.name}")
            player.networkData.server = server
            true
        } catch (e: BindException) {
            LOGGER.info("server port is already bound")
            player.networkData.server = null
            false
        } catch (e: Exception) {
            e.printStackTrace()
            player.networkData.server = null
            false
        }
    }

    private val LOGGER = LogManager.getLogger(MCProtocol::class)

}