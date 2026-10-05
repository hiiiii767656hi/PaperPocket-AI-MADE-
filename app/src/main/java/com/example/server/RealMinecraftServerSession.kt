package com.example.server

import android.util.Log
import com.example.data.local.entity.ServerProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class RealMinecraftServerSession(
    private val socket: Socket,
    private val dis: DataInputStream,
    private val dos: DataOutputStream,
    val clientProtocol: Int,
    private val engine: ServerEngine,
    private val profile: ServerProfileEntity
) {
    val sessionId: String = UUID.randomUUID().toString().substring(0, 8)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val isRunning = AtomicBoolean(true)

    var playerName: String = "Player"
        private set
    var playerUuid: UUID = UUID.randomUUID()
        private set
    var entityId: Int = 100
        private set
    var posX: Double = 0.5
    var posY: Double = 65.0
    var posZ: Double = 0.5
    var yaw: Float = 0f
    var pitch: Float = 0f
    var isCreative: Boolean = profile.gamemode.equals("creative", ignoreCase = true)
    var clientPingMs: Int = 20

    fun start() {
        scope.launch {
            try {
                handleLoginSequence()
                if (!isRunning.get()) return@launch

                engine.registerClientSession(this@RealMinecraftServerSession)

                // Start game: send join game, chunks, spawn position, player position
                sendJoinGame()
                sendPlayerAbilities()
                sendSetCenterChunk()
                sendSpawnChunks()
                sendPlayerPositionAndLook()
                sendDefaultSpawnPosition()

                // Welcome message
                sendSystemChatMessage("§a✔ Connected to §b${profile.name}§a on Android!")
                sendSystemChatMessage("§eWelcome §f$playerName§e! In-App Native 3D Server is running for real.")
                sendSystemChatMessage("§7Gamemode: §b${if (isCreative) "Creative" else "Survival"} §7• Type §b/help§7 for commands.")

                // Launch keep-alive loop
                launchKeepAliveLoop()

                // Read incoming packets from client
                readPacketLoop()
            } catch (e: Exception) {
                Log.d("RealMinecraftSession", "Session error: ${e.message}")
            } finally {
                closeSession()
            }
        }
    }

    private fun handleLoginSequence() {
        // Read Login Start packet (Packet ID 0x00 in Login state)
        val loginPacketLen = readVarInt(dis)
        if (loginPacketLen <= 0) {
            isRunning.set(false)
            return
        }
        val loginPacketId = readVarInt(dis)
        if (loginPacketId != 0x00) {
            isRunning.set(false)
            return
        }

        val nameLen = readVarInt(dis)
        val nameBytes = ByteArray(nameLen)
        dis.readFully(nameBytes)
        playerName = String(nameBytes, Charsets.UTF_8).trim()
        playerUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray(Charsets.UTF_8))

        // Read modern UUID if sent in packet
        if (clientProtocol >= 761) {
            try {
                val hasUuid = dis.readBoolean()
                if (hasUuid) {
                    val most = dis.readLong()
                    val least = dis.readLong()
                    playerUuid = UUID(most, least)
                }
            } catch (_: Exception) {}
        }

        // Send Login Success (Packet ID 0x02)
        val lsData = ByteArrayOutputStream()
        val lsDos = DataOutputStream(lsData)
        writeVarInt(lsDos, 0x02) // Login Success
        if (clientProtocol < 707) {
            writeString(lsDos, playerUuid.toString())
            writeString(lsDos, playerName)
        } else {
            lsDos.writeLong(playerUuid.mostSignificantBits)
            lsDos.writeLong(playerUuid.leastSignificantBits)
            writeString(lsDos, playerName)
            if (clientProtocol >= 759) {
                writeVarInt(lsDos, 0) // 0 properties
            }
            if (clientProtocol >= 766) {
                lsDos.writeBoolean(false) // strict error handling
            }
        }
        sendPacket(lsData.toByteArray())

        // Modern 1.20.2+ Configuration state
        if (clientProtocol >= 764) {
            handleConfigurationState()
        }
    }

    private fun handleConfigurationState() {
        // Client sends Login Acknowledged (0x03)
        var waitingForAck = true
        while (waitingForAck && isRunning.get()) {
            val len = readVarInt(dis)
            val id = readVarInt(dis)
            if (id == 0x03) {
                waitingForAck = false
            } else {
                // skip other packets if any
                val remaining = len - getVarIntSize(id)
                if (remaining > 0) dis.skipBytes(remaining)
            }
        }

        // Now in Configuration State
        // Send Finish Configuration packet
        val finishPacketId = if (clientProtocol >= 766) 0x03 else 0x02
        val cfgData = ByteArrayOutputStream()
        val cfgDos = DataOutputStream(cfgData)
        writeVarInt(cfgDos, finishPacketId)
        sendPacket(cfgData.toByteArray())

        // Wait for client to send Finish Configuration Acknowledged (0x02)
        var waitingForFinish = true
        while (waitingForFinish && isRunning.get()) {
            val len = readVarInt(dis)
            val id = readVarInt(dis)
            if (id == 0x02) {
                waitingForFinish = false
            } else {
                val remaining = len - getVarIntSize(id)
                if (remaining > 0) dis.skipBytes(remaining)
            }
        }
        // Now entering PLAY state!
    }

    private fun sendJoinGame() {
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)

        val packetId = when {
            clientProtocol >= 767 -> 0x2B
            clientProtocol >= 764 -> 0x29
            clientProtocol >= 762 -> 0x28
            clientProtocol >= 759 -> 0x25
            clientProtocol >= 754 -> 0x26
            clientProtocol >= 340 -> 0x23
            else -> 0x01 // 1.8.8
        }
        writeVarInt(dosP, packetId)

        if (clientProtocol < 754) {
            // 1.8 - 1.12.2 legacy join
            dosP.writeInt(entityId)
            dosP.writeByte(if (isCreative) 1 else 0)
            if (clientProtocol == 47) dosP.writeByte(0) else dosP.writeInt(0) // Overworld
            dosP.writeByte(1) // Difficulty Easy
            dosP.writeByte(profile.maxPlayers)
            writeString(dosP, "flat")
            dosP.writeBoolean(false)
        } else {
            // Modern 1.16 - 1.21 Join Game
            dosP.writeInt(entityId)
            dosP.writeBoolean(profile.hardcore)
            dosP.writeByte(if (isCreative) 1 else 0) // Gamemode
            dosP.writeByte(-1) // Previous gamemode

            // Dimension names
            writeVarInt(dosP, 1)
            writeString(dosP, "minecraft:overworld")

            // Registry Codec (NBT Compound)
            val codecBytes = NbtCompound.buildDimensionCodec().toByteArray()
            dosP.write(codecBytes)

            // Current dimension
            writeString(dosP, "minecraft:overworld")
            writeString(dosP, "minecraft:overworld")

            dosP.writeLong(0L) // Hashed seed
            writeVarInt(dosP, profile.maxPlayers)
            writeVarInt(dosP, profile.viewDistance.coerceIn(4, 16))
            writeVarInt(dosP, profile.simulationDistance.coerceIn(4, 16))
            dosP.writeBoolean(false) // Reduced debug info
            dosP.writeBoolean(true) // Enable respawn screen
            dosP.writeBoolean(false) // Is debug
            dosP.writeBoolean(true) // Is flat world

            // Modern 1.19+ death location flag
            if (clientProtocol >= 759) {
                dosP.writeBoolean(false) // Has death location
            }
            // Modern 1.20+ portal cooldown
            if (clientProtocol >= 763) {
                writeVarInt(dosP, 0) // Portal cooldown
            }
        }

        sendPacket(data.toByteArray())
    }

    private fun sendPlayerAbilities() {
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)

        val packetId = when {
            clientProtocol >= 767 -> 0x36
            clientProtocol >= 764 -> 0x34
            clientProtocol >= 754 -> 0x32
            clientProtocol >= 340 -> 0x2C
            else -> 0x39
        }
        writeVarInt(dosP, packetId)

        // Flags: 0x01: invulnerable, 0x02: flying, 0x04: can fly, 0x08: instabuild (creative)
        val flags: Byte = if (isCreative) 0x0F else 0x00
        dosP.writeByte(flags.toInt())
        dosP.writeFloat(0.05f) // Flying speed
        dosP.writeFloat(0.1f) // Walking / FOV speed

        sendPacket(data.toByteArray())
    }

    private fun sendSetCenterChunk() {
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)

        val packetId = when {
            clientProtocol >= 767 -> 0x56
            clientProtocol >= 764 -> 0x52
            clientProtocol >= 754 -> 0x4E
            else -> return // legacy does not require center chunk
        }
        writeVarInt(dosP, packetId)
        writeVarInt(dosP, 0) // Chunk X = 0
        writeVarInt(dosP, 0) // Chunk Z = 0

        sendPacket(data.toByteArray())
    }

    private fun sendDefaultSpawnPosition() {
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)

        val packetId = when {
            clientProtocol >= 767 -> 0x58
            clientProtocol >= 764 -> 0x54
            clientProtocol >= 754 -> 0x50
            clientProtocol >= 340 -> 0x46
            else -> 0x05
        }
        writeVarInt(dosP, packetId)

        if (clientProtocol >= 754) {
            // Position as Long: ((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)
            val posLong = ((0L and 0x3FFFFFFL) shl 38) or ((0L and 0x3FFFFFFL) shl 12) or (65L and 0xFFFL)
            dosP.writeLong(posLong)
            dosP.writeFloat(0f) // Angle
        } else {
            // Legacy Position
            val posLong = ((0L and 0x3FFFFFFL) shl 38) or ((65L and 0xFFFL) shl 26) or (0L and 0x3FFFFFFL)
            dosP.writeLong(posLong)
        }

        sendPacket(data.toByteArray())
    }

    private fun sendPlayerPositionAndLook() {
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)

        val packetId = when {
            clientProtocol >= 767 -> 0x40
            clientProtocol >= 764 -> 0x3E
            clientProtocol >= 762 -> 0x3C
            clientProtocol >= 759 -> 0x39
            clientProtocol >= 754 -> 0x38
            clientProtocol >= 340 -> 0x2F
            else -> 0x08
        }
        writeVarInt(dosP, packetId)

        dosP.writeDouble(posX)
        dosP.writeDouble(posY)
        dosP.writeDouble(posZ)
        dosP.writeFloat(yaw)
        dosP.writeFloat(pitch)
        dosP.writeByte(0) // Relative flags = 0 (absolute coordinates)
        if (clientProtocol >= 754) {
            writeVarInt(dosP, 1) // Teleport ID
            if (clientProtocol >= 762) {
                dosP.writeBoolean(false) // Dismount vehicle
            }
        }

        sendPacket(data.toByteArray())
    }

    private fun sendSpawnChunks() {
        // Send a 3x3 grid of solid chunk columns centered around spawn (0, 0)
        for (cx in -1..1) {
            for (cz in -1..1) {
                sendSingleChunk(cx, cz)
            }
        }
    }

    private fun sendSingleChunk(chunkX: Int, chunkZ: Int) {
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)

        val packetId = when {
            clientProtocol >= 767 -> 0x29
            clientProtocol >= 764 -> 0x27
            clientProtocol >= 762 -> 0x24
            clientProtocol >= 759 -> 0x21
            clientProtocol >= 754 -> 0x20
            clientProtocol >= 340 -> 0x20
            else -> 0x21
        }
        writeVarInt(dosP, packetId)

        if (clientProtocol >= 754) {
            // Modern Chunk packet
            dosP.writeInt(chunkX)
            dosP.writeInt(chunkZ)

            // Heightmaps (minimal empty NBT compound)
            dosP.writeByte(10) // TAG_Compound
            dosP.writeByte(0)  // TAG_End
            dosP.writeByte(0)

            // Construct chunk sections (24 sections for Y=-64 to Y=319)
            val chunkSectionsBytes = buildModernChunkSections()
            writeVarInt(dosP, chunkSectionsBytes.size)
            dosP.write(chunkSectionsBytes)

            // Block entities count = 0
            writeVarInt(dosP, 0)

            // Light Data (1.18+)
            if (clientProtocol >= 757) {
                dosP.writeBoolean(true) // Trust edges
                // Sky Light Mask BitSet (0 longs)
                writeVarInt(dosP, 0)
                // Block Light Mask BitSet (0 longs)
                writeVarInt(dosP, 0)
                // Empty Sky Light Mask BitSet (0 longs)
                writeVarInt(dosP, 0)
                // Empty Block Light Mask BitSet (0 longs)
                writeVarInt(dosP, 0)
                // Sky Light Arrays count = 0
                writeVarInt(dosP, 0)
                // Block Light Arrays count = 0
                writeVarInt(dosP, 0)
            }
        } else {
            // Legacy 1.8 - 1.12 Chunk packet (Simplified full flat chunk)
            dosP.writeInt(chunkX)
            dosP.writeInt(chunkZ)
            dosP.writeBoolean(true) // Full chunk
            dosP.writeShort(0x0001) // Bitmask section 0
            val legacyBytes = ByteArray(16 * 16 * 16 * 2 + 256)
            // Fill bottom section with grass (id 2)
            for (i in 0 until 16 * 16) {
                legacyBytes[i] = 2.toByte() // Grass block
            }
            writeVarInt(dosP, legacyBytes.size)
            dosP.write(legacyBytes)
        }

        sendPacket(data.toByteArray())
    }

    private fun buildModernChunkSections(): ByteArray {
        val baos = ByteArrayOutputStream()
        val dosS = DataOutputStream(baos)

        // 24 chunk sections from bottom (section 0 = Y -64..-49) to top (section 23 = Y 304..319)
        // Section 0..3: Stone (-64..-1)
        // Section 4..6: Stone/Dirt (0..47)
        // Section 7: Dirt (48..63)
        // Section 8: Grass Block (64..79)
        // Section 9..23: Air (80..319)
        for (sectionY in 0 until 24) {
            when {
                sectionY in 0..6 -> {
                    // Solid Stone Section
                    dosS.writeShort(4096) // Block count
                    // Block States: bitsPerEntry = 0 (single value)
                    dosS.writeByte(0)
                    writeVarInt(dosS, 1) // Stone state ID
                    writeVarInt(dosS, 0) // Data array length = 0
                    // Biomes: bitsPerEntry = 0
                    dosS.writeByte(0)
                    writeVarInt(dosS, 0) // Plains biome
                    writeVarInt(dosS, 0)
                }
                sectionY == 7 -> {
                    // Solid Dirt Section
                    dosS.writeShort(4096)
                    dosS.writeByte(0)
                    writeVarInt(dosS, 10) // Dirt state ID
                    writeVarInt(dosS, 0)
                    dosS.writeByte(0)
                    writeVarInt(dosS, 0)
                    writeVarInt(dosS, 0)
                }
                sectionY == 8 -> {
                    // Solid Grass Section
                    dosS.writeShort(4096)
                    dosS.writeByte(0)
                    writeVarInt(dosS, 9) // Grass block state ID
                    writeVarInt(dosS, 0)
                    dosS.writeByte(0)
                    writeVarInt(dosS, 0)
                    writeVarInt(dosS, 0)
                }
                else -> {
                    // Empty Air Section
                    dosS.writeShort(0) // Block count = 0
                    dosS.writeByte(0)
                    writeVarInt(dosS, 0) // Air state ID = 0
                    writeVarInt(dosS, 0)
                    dosS.writeByte(0)
                    writeVarInt(dosS, 0)
                    writeVarInt(dosS, 0)
                }
            }
        }
        dosS.flush()
        return baos.toByteArray()
    }

    fun sendSystemChatMessage(message: String) {
        if (!isRunning.get()) return
        scope.launch {
            try {
                val data = ByteArrayOutputStream()
                val dosP = DataOutputStream(data)

                val packetId = when {
                    clientProtocol >= 767 -> 0x6C
                    clientProtocol >= 764 -> 0x69
                    clientProtocol >= 762 -> 0x67
                    clientProtocol >= 759 -> 0x60
                    clientProtocol >= 754 -> 0x0E
                    clientProtocol >= 340 -> 0x0F
                    else -> 0x02
                }
                writeVarInt(dosP, packetId)

                val chatJson = JSONObject()
                chatJson.put("text", message)
                writeString(dosP, chatJson.toString())

                if (clientProtocol >= 759) {
                    dosP.writeBoolean(false) // Not an action bar overlay
                } else if (clientProtocol >= 754) {
                    dosP.writeByte(0) // Position: Chat (0)
                    dosP.writeLong(0L) // Sender UUID
                    dosP.writeLong(0L)
                } else {
                    dosP.writeByte(0) // Position
                }

                sendPacket(data.toByteArray())
            } catch (e: Exception) {
                Log.d("RealMinecraftSession", "Failed to send chat: ${e.message}")
            }
        }
    }

    fun setGamemode(creative: Boolean) {
        isCreative = creative
        sendPlayerAbilities()

        // Send Game State Change packet (Change Gamemode = 3)
        val data = ByteArrayOutputStream()
        val dosP = DataOutputStream(data)
        val packetId = when {
            clientProtocol >= 767 -> 0x22
            clientProtocol >= 764 -> 0x20
            clientProtocol >= 754 -> 0x1E
            clientProtocol >= 340 -> 0x1E
            else -> 0x2B
        }
        writeVarInt(dosP, packetId)
        dosP.writeByte(3) // Change game mode
        dosP.writeFloat(if (creative) 1f else 0f)
        sendPacket(data.toByteArray())

        sendSystemChatMessage("§aYour game mode has been updated to §b${if (creative) "Creative" else "Survival"} Mode§a.")
    }

    fun teleport(x: Double, y: Double, z: Double) {
        posX = x
        posY = y
        posZ = z
        sendPlayerPositionAndLook()
        sendSystemChatMessage("§aTeleported to §f($x, $y, $z)§a.")
    }

    fun kick(reason: String) {
        if (!isRunning.get()) return
        scope.launch {
            try {
                val data = ByteArrayOutputStream()
                val dosP = DataOutputStream(data)
                val packetId = when {
                    clientProtocol >= 767 -> 0x1D
                    clientProtocol >= 764 -> 0x1B
                    clientProtocol >= 754 -> 0x1A
                    clientProtocol >= 340 -> 0x1A
                    else -> 0x40
                }
                writeVarInt(dosP, packetId)
                val kickObj = JSONObject()
                kickObj.put("text", reason)
                writeString(dosP, kickObj.toString())
                sendPacket(data.toByteArray())
            } catch (_: Exception) {}
            closeSession()
        }
    }

    private fun launchKeepAliveLoop() {
        scope.launch {
            while (isActive && isRunning.get()) {
                delay(8000)
                try {
                    val keepAliveId = System.currentTimeMillis()
                    val data = ByteArrayOutputStream()
                    val dosP = DataOutputStream(data)
                    val packetId = when {
                        clientProtocol >= 767 -> 0x26
                        clientProtocol >= 764 -> 0x24
                        clientProtocol >= 754 -> 0x23
                        clientProtocol >= 340 -> 0x1F
                        else -> 0x00
                    }
                    writeVarInt(dosP, packetId)
                    if (clientProtocol >= 340) {
                        dosP.writeLong(keepAliveId)
                    } else {
                        writeVarInt(dosP, keepAliveId.toInt())
                    }
                    sendPacket(data.toByteArray())
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun readPacketLoop() {
        while (isRunning.get() && !socket.isClosed) {
            val length = try {
                readVarInt(dis)
            } catch (e: Exception) {
                break
            }
            if (length <= 0) continue

            val packetId = readVarInt(dis)
            val payloadLen = length - getVarIntSize(packetId)

            handleIncomingPlayPacket(packetId, payloadLen)
        }
    }

    private fun handleIncomingPlayPacket(packetId: Int, payloadLen: Int) {
        try {
            when (packetId) {
                // Keep Alive Response
                0x15, 0x14, 0x12, 0x10, 0x0F, 0x0B -> {
                    if (payloadLen >= 8) {
                        val pingTimestamp = dis.readLong()
                        val diff = (System.currentTimeMillis() - pingTimestamp).toInt().coerceIn(5, 500)
                        clientPingMs = diff
                        engine.updatePlayerPing(playerName, clientPingMs)
                    } else if (payloadLen > 0) {
                        dis.skipBytes(payloadLen)
                    }
                }

                // Player Position / Movement
                0x14, 0x17, 0x1A, 0x1D -> {
                    if (payloadLen >= 24) {
                        posX = dis.readDouble()
                        posY = dis.readDouble()
                        posZ = dis.readDouble()
                        val remaining = payloadLen - 24
                        if (remaining > 0) dis.skipBytes(remaining)
                        engine.updatePlayerPosition(playerName, posX, posY, posZ, yaw, pitch)
                    } else if (payloadLen > 0) {
                        dis.skipBytes(payloadLen)
                    }
                }

                // Chat Message / Command
                0x04, 0x05, 0x03, 0x02 -> {
                    if (payloadLen > 0) {
                        val text = try {
                            val strLen = readVarInt(dis)
                            val strBytes = ByteArray(strLen.coerceAtMost(256))
                            dis.readFully(strBytes)
                            val remaining = payloadLen - getVarIntSize(strLen) - strBytes.size
                            if (remaining > 0) dis.skipBytes(remaining)
                            String(strBytes, Charsets.UTF_8).trim()
                        } catch (_: Exception) {
                            ""
                        }

                        if (text.isNotBlank()) {
                            handlePlayerChat(text)
                        }
                    }
                }

                else -> {
                    if (payloadLen > 0) {
                        dis.skipBytes(payloadLen)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore corrupted client payload and keep alive
        }
    }

    private fun handlePlayerChat(text: String) {
        if (text.startsWith("/")) {
            // Player command
            val cmd = text.substring(1).trim()
            engine.logPlayerCommand(playerName, cmd)
            val parts = cmd.split(" ").filter { it.isNotBlank() }
            val root = parts.getOrNull(0)?.lowercase(Locale.ROOT) ?: return
            val args = parts.drop(1)

            when (root) {
                "help", "?" -> {
                    sendSystemChatMessage("§a=== In-App Server Commands ===")
                    sendSystemChatMessage("§e/gamemode <creative|survival> §7- Switch mode")
                    sendSystemChatMessage("§e/time set <day|night> §7- Change time")
                    sendSystemChatMessage("§e/tp <x> <y> <z> §7- Teleport")
                    sendSystemChatMessage("§e/ping §7- View connection latency")
                    sendSystemChatMessage("§e/say <msg> §7- Broadcast message")
                }
                "gamemode", "gm" -> {
                    val mode = args.getOrNull(0)?.lowercase(Locale.ROOT) ?: "creative"
                    if (mode.contains("c") || mode == "1") {
                        setGamemode(true)
                    } else {
                        setGamemode(false)
                    }
                }
                "ping" -> {
                    sendSystemChatMessage("§aPong! Latency: §b${clientPingMs}ms")
                }
                "say" -> {
                    val msg = args.joinToString(" ")
                    engine.broadcastMessage("§d[$playerName] $msg")
                }
                "time" -> {
                    val sub = args.getOrNull(1) ?: args.getOrNull(0) ?: "day"
                    engine.executeCommand("time set $sub")
                    sendSystemChatMessage("§aWorld time updated to §b$sub§a.")
                }
                "tp" -> {
                    if (args.size >= 3) {
                        val x = args[0].toDoubleOrNull() ?: posX
                        val y = args[1].toDoubleOrNull() ?: posY
                        val z = args[2].toDoubleOrNull() ?: posZ
                        teleport(x, y, z)
                    } else {
                        sendSystemChatMessage("§cUsage: /tp <x> <y> <z>")
                    }
                }
                else -> {
                    engine.executeCommand(cmd)
                }
            }
        } else {
            // Normal public chat
            engine.broadcastPlayerChat(playerName, text)
        }
    }

    @Synchronized
    private fun sendPacket(data: ByteArray) {
        if (!isRunning.get() || socket.isClosed) return
        try {
            writeVarInt(dos, data.size)
            dos.write(data)
            dos.flush()
        } catch (e: Exception) {
            closeSession()
        }
    }

    fun closeSession() {
        if (isRunning.compareAndSet(true, false)) {
            engine.unregisterClientSession(this)
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    companion object {
        fun readVarInt(dis: DataInputStream): Int {
            var numRead = 0
            var result = 0
            var read: Byte
            do {
                read = dis.readByte()
                val value = (read.toInt() and 0x7F)
                result = result or (value shl (7 * numRead))
                numRead++
                if (numRead > 5) throw RuntimeException("VarInt is too big")
            } while ((read.toInt() and 0x80) != 0)
            return result
        }

        fun writeVarInt(dos: DataOutputStream, value: Int) {
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

        fun writeString(dos: DataOutputStream, str: String) {
            val bytes = str.toByteArray(Charsets.UTF_8)
            writeVarInt(dos, bytes.size)
            dos.write(bytes)
        }

        fun getVarIntSize(value: Int): Int {
            var v = value
            var size = 0
            do {
                v = v ushr 7
                size++
            } while (v != 0)
            return size
        }
    }
}
