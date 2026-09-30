package com.example.server

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING
}

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    DEBUG,
    COMMAND
}

data class ConsoleMessage(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "Server",
    val text: String
)

data class LivePlayer(
    val name: String,
    val uuid: String,
    val pingMs: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val dimension: String = "minecraft:overworld",
    val gamemode: String = "SURVIVAL",
    val health: Float = 20f,
    val food: Int = 20,
    val isOp: Boolean = false
)

data class ServerMetrics(
    val tps: Double = 20.0,
    val mspt: Double = 14.2,
    val usedMemoryMb: Long = 480,
    val allocatedMemoryMb: Long = 1024,
    val maxMemoryMb: Long = 2048,
    val cpuPercent: Int = 18,
    val onlinePlayers: Int = 0,
    val maxPlayers: Int = 20,
    val chunksLoaded: Int = 441,
    val uptimeSeconds: Long = 0,
    val networkRxKbps: Double = 1.2,
    val networkTxKbps: Double = 2.4
)

data class ServerWorldInfo(
    val name: String,
    val difficulty: String,
    val seed: String,
    val timeOfDay: Long, // 0 - 24000
    val weather: String, // CLEAR, RAIN, THUNDER
    val sizeBytes: Long
)
