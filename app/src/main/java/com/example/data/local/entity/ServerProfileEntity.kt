package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_profiles")
data class ServerProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val minecraftVersion: String = "26.3",
    val paperBuild: Int = 120,
    val port: Int = 25565,
    val motd: String = "§bPaperCraft §7On-Device Server §a[26.3]",
    val maxPlayers: Int = 20,
    val gamemode: String = "survival",
    val difficulty: String = "normal",
    val pvp: Boolean = true,
    val onlineMode: Boolean = false, // defaults false for easy phone play without auth proxies
    val allocatedRamMb: Int = 1024,
    val useAikarFlags: Boolean = true,
    val customJvmArgs: String = "-Dpaper.playerconnection.keepalive=30",
    val viewDistance: Int = 10,
    val simulationDistance: Int = 8,
    val whiteListEnabled: Boolean = false,
    val hardcore: Boolean = false,
    val allowFlight: Boolean = false,
    val spawnProtection: Int = 16,
    val levelName: String = "world",
    val seed: String = "",
    val enableLanBroadcast: Boolean = true,
    val enableBedrockPing: Boolean = true,
    val isDefault: Boolean = false,
    // Custom Paper build selection:
    val customJarFileName: String? = null,
    val customJarUri: String? = null,
    val customJarSizeBytes: Long = 0L,
    val useCustomJar: Boolean = false,
    val importedFromZip: Boolean = false,
    val originalStartScript: String? = null,
    val customServerDirectory: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEffectiveDirectory(context: android.content.Context): java.io.File {
        if (!customServerDirectory.isNullOrBlank()) {
            val customDir = java.io.File(customServerDirectory)
            if (!customDir.exists()) customDir.mkdirs()
            return customDir
        }
        val defaultDir = java.io.File(context.filesDir, "servers/$id")
        if (!defaultDir.exists()) defaultDir.mkdirs()
        return defaultDir
    }
}
