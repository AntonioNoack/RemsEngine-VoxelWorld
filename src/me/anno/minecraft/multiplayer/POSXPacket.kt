package me.anno.minecraft.multiplayer

import me.anno.Time
import me.anno.engine.Events.addEvent
import me.anno.minecraft.entity.Player
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.POS1Packet
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import java.io.DataInputStream
import java.io.DataOutputStream

class POSXPacket : POS1Packet("POSX") {

    override val constantSize = false
    override val size: Int
        get() = super.size + 2 + name.encodeToByteArray().size

    var name = ""

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeUTF(name)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        name = dis.readUTF()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        client as MCClient
        val players = client.network.players
        val player = synchronized(players) { players[name] }
        if (player == null) {
            LOGGER.info("Missing player $name | ${System.identityHashCode(players)} | ${players.keys}")
            return
        }
        val entity = player.entity
        if (!player.isPrimary && entity != null) {
            // save all variables, because this packet instance will be reused
            val px = px
            val py = py
            val pz = pz
            val rx = rx
            val ry = ry
            val rz = rz
            val rw = rw
            // make sure of sync execution
            addEvent {
                val transform = entity.transform
                transform.localPosition =
                    transform.localPosition.set(px, py, pz)
                transform.localRotation =
                    transform.localRotation.set(rx, ry, rz, rw)
                transform.entity?.invalidateAABBsCompletely()
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
            packet.name = player.name
            packet.px = pos.x
            packet.py = pos.y
            packet.pz = pos.z
            val rot = entity.transform.globalTransform
                .getUnnormalizedRotation(JomlPools.quat4f.borrow())
            packet.rx = rot.x
            packet.ry = rot.y
            packet.rz = rot.z
            packet.rw = rot.w
            packet.entity = client?.randomId?.toLong() ?: 0L
            packet.localTime = Time.gameTimeN
            // println("sent $pos for ${player.name}")
            return packet
        }

    }
}