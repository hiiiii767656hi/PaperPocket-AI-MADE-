package com.example.server

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class BedrockLanBroadcaster(
    private val serverName: String,
    private val port: Int,
    private val maxPlayers: Int
) {
    private var isRunning = false
    private var broadcastThread: Thread? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        broadcastThread = Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val destination = InetAddress.getByName("255.255.255.255")
                val serverId = System.currentTimeMillis()

                while (isRunning) {
                    val message = "MCPE;$serverName;685;1.21.0;0;$maxPlayers;$serverId;PaperHost Mobile;Survival;1;$port;$port;"
                    val data = message.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(data, data.size, destination, 19132)
                    socket.send(packet)
                    Thread.sleep(1500)
                }
            } catch (e: Exception) {
                Log.d("BedrockBroadcaster", "Broadcast loop ended: ${e.message}")
            } finally {
                socket?.close()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning = false
        broadcastThread?.interrupt()
        broadcastThread = null
    }
}
