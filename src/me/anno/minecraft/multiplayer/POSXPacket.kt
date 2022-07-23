package me.anno.minecraft.multiplayer

import me.anno.Engine
import me.anno.minecraft.entity.Player
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.POS1Packet
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager

class POSXPacket : POS1Packet("POSX") {

    override fun onReceive(server: Server?, client: TCPClient) {
        client as MCClient
        val players = client.network.players
        val player = synchronized(players) { players[client.name] }
        if (player == null) {
            LOGGER.warn("Missing player ${client.name}")
            return
        }
        val entity = player.entity
        if (player.isPrimary && entity != null) {
            // save all variables, because this packet instance will be reused
            val px = px
            val py = py
            val pz = pz
            val rx = rx.toDouble()
            val ry = ry.toDouble()
            val rz = rz.toDouble()
            val rw = rw.toDouble()
            // make sure of sync execution
            addEvent {
                val transform = entity.transform
                transform.localPosition =
                    transform.localPosition.set(px, py, pz)
                transform.localRotation =
                    transform.localRotation.set(rx, ry, rz, rw)
                transform.smoothUpdate()
            }
        }
    }

    override val className: String = "POSXPacket"

    companion object {

        private val LOGGER = LogManager.getLogger(POSXPacket::class)

        fun createPacket(player: Player, client: TCPClient?): POSXPacket {
            val entity = player.entity!!
            val pos = entity.transform.globalPosition
            val packet = POSXPacket()
            packet.px = pos.x
            packet.py = pos.y
            packet.pz = pos.z
            val rot = entity.transform.globalTransform
                .getUnnormalizedRotation(JomlPools.quat4d.borrow())
            packet.rx = rot.x.toFloat()
            packet.ry = rot.y.toFloat()
            packet.rz = rot.z.toFloat()
            packet.rw = rot.w.toFloat()
            packet.entity = client?.randomId?.toLong() ?: 0L
            packet.localTime = Engine.gameTime
            // println("sent $pos for ${player.name}")
            return packet
        }

    }
}