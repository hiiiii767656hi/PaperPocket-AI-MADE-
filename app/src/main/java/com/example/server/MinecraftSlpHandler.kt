package com.example.server

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object MinecraftSlpHandler {

    fun handleClient(
        socket: Socket,
        serverName: String,
        versionName: String,
        protocolVersion: Int,
        motd: String,
        onlinePlayers: Int,
        maxPlayers: Int,
        samplePlayers: List<String>
    ) {
        socket.use { s ->
            s.soTimeout = 7000
            val input = s.getInputStream()
            val output = s.getOutputStream()

            val dis = DataInputStream(input)
            val dos = DataOutputStream(output)

            // Read Handshake packet
            val handshakeLength = readVarInt(dis)
            if (handshakeLength <= 0) return
            val packetId = readVarInt(dis)
            if (packetId != 0x00) return

            val clientProtocol = readVarInt(dis)
            val hostLength = readVarInt(dis)
            val hostBytes = ByteArray(hostLength)
            dis.readFully(hostBytes)
            val port = dis.readUnsignedShort()
            val nextState = readVarInt(dis)

            if (nextState == 1) { // Status state
                // Next packet from client is Status Request (0x00)
                val statusReqLength = readVarInt(dis)
                val statusReqId = readVarInt(dis)
                if (statusReqId != 0x00) return

                // Prepare JSON response
                val json = JSONObject()
                val versionObj = JSONObject()
                versionObj.put("name", versionName)
                versionObj.put("protocol", protocolVersion)
                json.put("version", versionObj)

                val playersObj = JSONObject()
                playersObj.put("max", maxPlayers)
                playersObj.put("online", onlinePlayers)
                val sampleArray = JSONArray()
                samplePlayers.forEach { name ->
                    val p = JSONObject()
                    p.put("name", name)
                    p.put("id", "00000000-0000-0000-0000-000000000001")
                    sampleArray.put(p)
                }
                playersObj.put("sample", sampleArray)
                json.put("players", playersObj)

                val descObj = JSONObject()
                descObj.put("text", MinecraftColorFormatter.stripColor(motd))
                json.put("description", descObj)
                json.put("enforcesSecureChat", false)

                val jsonString = json.toString()
                val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

                val packetData = ByteArrayOutputStream()
                val packetDos = DataOutputStream(packetData)
                writeVarInt(packetDos, 0x00) // Packet ID 0
                writeVarInt(packetDos, jsonBytes.size) // String length
                packetDos.write(jsonBytes)

                val fullPacket = packetData.toByteArray()
                writeVarInt(dos, fullPacket.size)
                dos.write(fullPacket)
                dos.flush()

                // Now client might send Ping packet (Packet ID 0x01 with 8 byte long payload)
                try {
                    val pingLength = readVarInt(dis)
                    val pingId = readVarInt(dis)
                    if (pingId == 0x01) {
                        val payload = dis.readLong()

                        // Respond with Pong (Packet ID 0x01 with same long)
                        val pongData = ByteArrayOutputStream()
                        val pongDos = DataOutputStream(pongData)
                        writeVarInt(pongDos, 0x01)
                        pongDos.writeLong(payload)

                        val fullPong = pongData.toByteArray()
                        writeVarInt(dos, fullPong.size)
                        dos.write(fullPong)
                        dos.flush()
                    }
                } catch (_: Exception) {
                    // Client disconnected after status, normal in Minecraft
                }
            } else if (nextState == 2) {
                // Login state: If client tries to join
                val loginStartLength = readVarInt(dis)
                val loginStartId = readVarInt(dis)
                if (loginStartId == 0x00) {
                    val nameLen = readVarInt(dis)
                    val nameBytes = ByteArray(nameLen)
                    dis.readFully(nameBytes)
                    val playerName = String(nameBytes, Charsets.UTF_8)

                    // Send Disconnect packet with informative message
                    val kickReason = JSONObject()
                    kickReason.put(
                        "text",
                        "§b[PaperCraft On-Device]\n§eServer is running on this phone!\n§7Player §f$playerName §7attempted connection.\n§aManage players & game inside the PaperCraft app."
                    )
                    val kickBytes = kickReason.toString().toByteArray(Charsets.UTF_8)
                    val kickData = ByteArrayOutputStream()
                    val kickDos = DataOutputStream(kickData)
                    writeVarInt(kickDos, 0x00) // Disconnect packet
                    writeVarInt(kickDos, kickBytes.size)
                    kickDos.write(kickBytes)

                    val fullKick = kickData.toByteArray()
                    writeVarInt(dos, fullKick.size)
                    dos.write(fullKick)
                    dos.flush()
                }
            }
        }
    }

    private fun readVarInt(dis: DataInputStream): Int {
        var numRead = 0
        var result = 0
        var read: Byte
        do {
            read = dis.readByte()
            val value = (read.toInt() and 0x7F)
            result = result or (value shl (7 * numRead))
            numRead++
            if (numRead > 5) {
                throw RuntimeException("VarInt is too big")
            }
        } while ((read.toInt() and 0x80) != 0)
        return result
    }

    private fun writeVarInt(dos: DataOutputStream, value: Int) {
        var v = value
        do {
            var temp = (v and 0x7F)
            v = v ushr 7
            if (v != 0) {
                temp = temp or 0x80
            }
            dos.writeByte(temp)
        } while (v != 0)
    }
}
