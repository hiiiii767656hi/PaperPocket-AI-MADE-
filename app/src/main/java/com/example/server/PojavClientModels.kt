package com.example.server

data class PojavClientState(
    val isRunning: Boolean = false,
    val isConnectedToServer: Boolean = false,
    val username: String = "Pojav_Steve",
    val minecraftVersion: String = "1.21.1",
    val selectedJavaRuntime: String = "Internal OpenJDK 21 (LTS)",
    val allocatedRamMb: Int = 2048,
    val renderer: String = "GL4ES 1.1.4 (OpenGL 3.2)",
    val posX: Double = 0.5,
    val posY: Double = 65.0,
    val posZ: Double = 0.5,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val health: Float = 20f,
    val food: Int = 20,
    val level: Int = 42,
    val gamemode: String = "SURVIVAL",
    val pingMs: Int = 14,
    val isF3Open: Boolean = false,
    val isChatOpen: Boolean = false,
    val isInventoryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val selectedHotbarSlot: Int = 0,
    val chatMessages: List<PojavChatMessage> = emptyList()
)

data class PojavChatMessage(
    val id: Long = System.nanoTime(),
    val sender: String,
    val text: String,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class HotbarItem(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val count: Int = 64
)

interface PojavSessionListener {
    fun onServerMessage(message: String)
    fun onGamemodeChanged(newGamemode: String)
    fun onTeleported(x: Double, y: Double, z: Double)
}
