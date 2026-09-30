package com.example.utils

import java.io.StringReader
import java.util.Properties

data class DetectedServerProperties(
    val port: Int = 25565,
    val motd: String = "§aImported Minecraft Server",
    val gamemode: String = "survival",
    val difficulty: String = "normal",
    val pvp: Boolean = true,
    val onlineMode: Boolean = false,
    val maxPlayers: Int = 20,
    val viewDistance: Int = 10,
    val simulationDistance: Int = 8,
    val levelName: String = "world",
    val seed: String = "",
    val whiteListEnabled: Boolean = false,
    val hardcore: Boolean = false,
    val allowFlight: Boolean = false,
    val spawnProtection: Int = 16
)

object ServerPropertiesParser {

    fun parse(content: String): DetectedServerProperties {
        val props = Properties()
        try {
            props.load(StringReader(content))
        } catch (_: Exception) {}

        return DetectedServerProperties(
            port = props.getProperty("server-port")?.trim()?.toIntOrNull() ?: 25565,
            motd = props.getProperty("motd")?.trim() ?: "§aImported Paper Server",
            gamemode = props.getProperty("gamemode")?.trim()?.lowercase() ?: "survival",
            difficulty = props.getProperty("difficulty")?.trim()?.lowercase() ?: "normal",
            pvp = props.getProperty("pvp")?.trim()?.toBooleanStrictOrNull() ?: true,
            onlineMode = props.getProperty("online-mode")?.trim()?.toBooleanStrictOrNull() ?: false,
            maxPlayers = props.getProperty("max-players")?.trim()?.toIntOrNull() ?: 20,
            viewDistance = props.getProperty("view-distance")?.trim()?.toIntOrNull() ?: 10,
            simulationDistance = props.getProperty("simulation-distance")?.trim()?.toIntOrNull() ?: 8,
            levelName = props.getProperty("level-name")?.trim() ?: "world",
            seed = props.getProperty("level-seed")?.trim() ?: "",
            whiteListEnabled = props.getProperty("white-list")?.trim()?.toBooleanStrictOrNull() ?: false,
            hardcore = props.getProperty("hardcore")?.trim()?.toBooleanStrictOrNull() ?: false,
            allowFlight = props.getProperty("allow-flight")?.trim()?.toBooleanStrictOrNull() ?: false,
            spawnProtection = props.getProperty("spawn-protection")?.trim()?.toIntOrNull() ?: 16
        )
    }
}
