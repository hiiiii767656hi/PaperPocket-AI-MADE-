package com.example.server

import android.util.Log
import com.example.data.local.entity.ServerProfileEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

object MinecraftSlpHandler {

    fun handleClient(
        socket: Socket,
        engine: ServerEngine,
        profile: ServerProfileEntity,
        serverName: String,
        versionName: String,
        protocolVersion: Int = 767,
        autoMatchClientProtocol: Boolean = true,
        motd: String,
        onlinePlayers: Int,
        maxPlayers: Int,
        samplePlayers: List<String>
    ) {
        try {
            socket.soTimeout = 15000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            val dis = DataInputStream(input)
            val dos = DataOutputStream(output)

            // Read Handshake packet
            val handshakeLength = readVarInt(dis)
            if (handshakeLength <= 0) {
                socket.close()
                return
            }
            val packetId = readVarInt(dis)
            if (packetId != 0x00) {
                socket.close()
                return
            }

            val clientProtocol = readVarInt(dis)
            val hostLength = readVarInt(dis)
            val hostBytes = ByteArray(hostLength)
            dis.readFully(hostBytes)
            val port = dis.readUnsignedShort()
            val nextState = readVarInt(dis)

            if (nextState == 1) { // Status state (Server List Ping)
                try {
                    // Next packet from client is Status Request (0x00)
                    val statusReqLength = readVarInt(dis)
                    val statusReqId = readVarInt(dis)
                    if (statusReqId != 0x00) {
                        socket.close()
                        return
                    }

                    // Mirror client protocol to avoid "Incompatible version" red X on client
                    val protocolToReport = if (autoMatchClientProtocol && clientProtocol > 0) {
                        clientProtocol
                    } else {
                        protocolVersion
                    }

                    val json = JSONObject()
                    val versionObj = JSONObject()
                    versionObj.put("name", versionName)
                    versionObj.put("protocol", protocolToReport)
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

                    // Optional Ping packet (Packet ID 0x01 with 8 byte long payload)
                    try {
                        val pingLength = readVarInt(dis)
                        val pingId = readVarInt(dis)
                        if (pingId == 0x01) {
                            val payload = dis.readLong()

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
                        // Client disconnected after status
                    }
                } finally {
                    try {
                        socket.close()
                    } catch (_: Exception) {}
                }
            } else if (nextState == 2) {
                // Login state: Client wants to connect and PLAY!
                // Remove socket timeout for ongoing play session
                socket.soTimeout = 0
                val session = RealMinecraftServerSession(
                    socket = socket,
                    dis = dis,
                    dos = dos,
                    clientProtocol = clientProtocol,
                    engine = engine,
                    profile = profile
                )
                session.start()
                // Do NOT close socket here; session maintains it!
            } else {
                socket.close()
            }
        } catch (e: Exception) {
            try {
                socket.close()
            } catch (_: Exception) {}
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
