package com.example.utils

import com.example.data.local.entity.ServerProfileEntity
import java.io.StringReader
import java.util.Date
import java.util.Properties

enum class PropertyCategory(val title: String, val description: String) {
    COMMON("Common Settings", "Essential network, MOTD, and gameplay configurations"),
    NETWORK("Network & Connectivity", "Port, status query, ping, and network compression"),
    GAMEPLAY("Gameplay & Difficulty", "Game mode, difficulty, hardcore, and PvP rules"),
    SECURITY("Security & Access", "Online mode authentication, whitelist, and flight permissions"),
    WORLD("World & Generation", "World name, seed, structures, nether, and spawn protection"),
    PERFORMANCE("Performance & Limits", "View distance, simulation distance, chunk sync, and player caps"),
    CUSTOM("Custom & Plugin Keys", "Additional or third-party properties found in the file")
}

data class ServerPropertiesDocument(
    // Common & Core
    val serverPort: Int = 25565,
    val motd: String = "§aA Minecraft Server",
    val difficulty: String = "normal",
    val gamemode: String = "survival",
    val maxPlayers: Int = 20,
    val pvp: Boolean = true,
    val onlineMode: Boolean = false,

    // Security & Access
    val whiteList: Boolean = false,
    val enforceWhitelist: Boolean = false,
    val hardcore: Boolean = false,
    val allowFlight: Boolean = false,
    val playerIdleTimeout: Int = 0,

    // World & Environment
    val levelName: String = "world",
    val levelSeed: String = "",
    val levelType: String = "minecraft:normal",
    val spawnMonsters: Boolean = true,
    val spawnAnimals: Boolean = true,
    val spawnNpcs: Boolean = true,
    val generateStructures: Boolean = true,
    val allowNether: Boolean = true,
    val spawnProtection: Int = 16,
    val forceGamemode: Boolean = false,

    // Performance & Mechanics
    val viewDistance: Int = 10,
    val simulationDistance: Int = 8,
    val syncChunkWrites: Boolean = true,
    val networkCompressionThreshold: Int = 256,
    val maxTickTime: Long = 60000L,
    val enableCommandBlock: Boolean = false,
    val enableStatus: Boolean = true,

    // Resource Pack
    val resourcePack: String = "",
    val resourcePackPrompt: String = "",
    val requireResourcePack: Boolean = false,

    // Other / Plugin / Unrecognized keys
    val customProperties: Map<String, String> = emptyMap(),
    val headerComments: List<String> = emptyList()
) {

    fun toPropertiesFileText(): String {
        val sb = StringBuilder()
        sb.append("# Minecraft server.properties\n")
        sb.append("# Generated & Managed by PaperHost on Android\n")
        sb.append("# ${Date()}\n\n")

        // Network
        sb.append("# Network & Connectivity\n")
        sb.append("server-port=$serverPort\n")
        sb.append("enable-status=$enableStatus\n")
        sb.append("network-compression-threshold=$networkCompressionThreshold\n")
        sb.append("max-tick-time=$maxTickTime\n\n")

        // Server Identity & MOTD
        sb.append("# Server Identity\n")
        sb.append("motd=$motd\n")
        sb.append("max-players=$maxPlayers\n\n")

        // Gameplay
        sb.append("# Gameplay Rules\n")
        sb.append("difficulty=$difficulty\n")
        sb.append("gamemode=$gamemode\n")
        sb.append("force-gamemode=$forceGamemode\n")
        sb.append("hardcore=$hardcore\n")
        sb.append("pvp=$pvp\n")
        sb.append("allow-flight=$allowFlight\n")
        sb.append("player-idle-timeout=$playerIdleTimeout\n\n")

        // Security
        sb.append("# Security & Authentication\n")
        sb.append("online-mode=$onlineMode\n")
        sb.append("white-list=$whiteList\n")
        sb.append("enforce-whitelist=$enforceWhitelist\n\n")

        // World
        sb.append("# World & Generation\n")
        sb.append("level-name=$levelName\n")
        sb.append("level-seed=$levelSeed\n")
        sb.append("level-type=$levelType\n")
        sb.append("allow-nether=$allowNether\n")
        sb.append("generate-structures=$generateStructures\n")
        sb.append("spawn-monsters=$spawnMonsters\n")
        sb.append("spawn-animals=$spawnAnimals\n")
        sb.append("spawn-npcs=$spawnNpcs\n")
        sb.append("spawn-protection=$spawnProtection\n")
        sb.append("enable-command-block=$enableCommandBlock\n\n")

        // Performance
        sb.append("# Performance & Chunks\n")
        sb.append("view-distance=$viewDistance\n")
        sb.append("simulation-distance=$simulationDistance\n")
        sb.append("sync-chunk-writes=$syncChunkWrites\n\n")

        // Resource pack
        if (resourcePack.isNotBlank()) {
            sb.append("# Resource Pack\n")
            sb.append("resource-pack=$resourcePack\n")
            if (resourcePackPrompt.isNotBlank()) {
                sb.append("resource-pack-prompt=$resourcePackPrompt\n")
            }
            sb.append("require-resource-pack=$requireResourcePack\n\n")
        }

        // Custom properties
        if (customProperties.isNotEmpty()) {
            sb.append("# Custom & Plugin Properties\n")
            customProperties.forEach { (k, v) ->
                sb.append("$k=$v\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    fun applyToProfile(profile: ServerProfileEntity): ServerProfileEntity {
        return profile.copy(
            port = serverPort,
            motd = motd,
            difficulty = difficulty,
            gamemode = gamemode,
            maxPlayers = maxPlayers,
            pvp = pvp,
            onlineMode = onlineMode,
            hardcore = hardcore,
            allowFlight = allowFlight,
            whiteListEnabled = whiteList,
            viewDistance = viewDistance,
            simulationDistance = simulationDistance,
            spawnProtection = spawnProtection,
            levelName = levelName,
            seed = levelSeed
        )
    }

    companion object {
        fun fromProfile(profile: ServerProfileEntity): ServerPropertiesDocument {
            return ServerPropertiesDocument(
                serverPort = profile.port,
                motd = profile.motd,
                difficulty = profile.difficulty,
                gamemode = profile.gamemode,
                maxPlayers = profile.maxPlayers,
                pvp = profile.pvp,
                onlineMode = profile.onlineMode,
                hardcore = profile.hardcore,
                allowFlight = profile.allowFlight,
                whiteList = profile.whiteListEnabled,
                viewDistance = profile.viewDistance,
                simulationDistance = profile.simulationDistance,
                spawnProtection = profile.spawnProtection,
                levelName = profile.levelName,
                levelSeed = profile.seed
            )
        }

        fun fromText(text: String): ServerPropertiesDocument {
            val props = Properties()
            try {
                props.load(StringReader(text))
            } catch (_: Exception) {}

            val recognizedKeys = setOf(
                "server-port", "motd", "difficulty", "gamemode", "max-players",
                "pvp", "online-mode", "white-list", "enforce-whitelist", "hardcore",
                "allow-flight", "player-idle-timeout", "level-name", "level-seed",
                "level-type", "spawn-monsters", "spawn-animals", "spawn-npcs",
                "generate-structures", "allow-nether", "spawn-protection",
                "enable-command-block", "view-distance", "simulation-distance",
                "sync-chunk-writes", "network-compression-threshold", "max-tick-time",
                "enable-status", "resource-pack", "resource-pack-prompt",
                "require-resource-pack", "force-gamemode"
            )

            val custom = mutableMapOf<String, String>()
            for (key in props.stringPropertyNames()) {
                if (key !in recognizedKeys && !key.startsWith("#")) {
                    props.getProperty(key)?.let { custom[key] = it }
                }
            }

            return ServerPropertiesDocument(
                serverPort = props.getProperty("server-port")?.trim()?.toIntOrNull() ?: 25565,
                motd = props.getProperty("motd")?.trim() ?: "§aA Minecraft Server",
                difficulty = props.getProperty("difficulty")?.trim()?.lowercase() ?: "normal",
                gamemode = props.getProperty("gamemode")?.trim()?.lowercase() ?: "survival",
                maxPlayers = props.getProperty("max-players")?.trim()?.toIntOrNull() ?: 20,
                pvp = props.getProperty("pvp")?.trim()?.toBooleanStrictOrNull() ?: true,
                onlineMode = props.getProperty("online-mode")?.trim()?.toBooleanStrictOrNull() ?: false,
                whiteList = props.getProperty("white-list")?.trim()?.toBooleanStrictOrNull() ?: false,
                enforceWhitelist = props.getProperty("enforce-whitelist")?.trim()?.toBooleanStrictOrNull() ?: false,
                hardcore = props.getProperty("hardcore")?.trim()?.toBooleanStrictOrNull() ?: false,
                allowFlight = props.getProperty("allow-flight")?.trim()?.toBooleanStrictOrNull() ?: false,
                playerIdleTimeout = props.getProperty("player-idle-timeout")?.trim()?.toIntOrNull() ?: 0,
                levelName = props.getProperty("level-name")?.trim() ?: "world",
                levelSeed = props.getProperty("level-seed")?.trim() ?: "",
                levelType = props.getProperty("level-type")?.trim() ?: "minecraft:normal",
                spawnMonsters = props.getProperty("spawn-monsters")?.trim()?.toBooleanStrictOrNull() ?: true,
                spawnAnimals = props.getProperty("spawn-animals")?.trim()?.toBooleanStrictOrNull() ?: true,
                spawnNpcs = props.getProperty("spawn-npcs")?.trim()?.toBooleanStrictOrNull() ?: true,
                generateStructures = props.getProperty("generate-structures")?.trim()?.toBooleanStrictOrNull() ?: true,
                allowNether = props.getProperty("allow-nether")?.trim()?.toBooleanStrictOrNull() ?: true,
                spawnProtection = props.getProperty("spawn-protection")?.trim()?.toIntOrNull() ?: 16,
                enableCommandBlock = props.getProperty("enable-command-block")?.trim()?.toBooleanStrictOrNull() ?: false,
                viewDistance = props.getProperty("view-distance")?.trim()?.toIntOrNull() ?: 10,
                simulationDistance = props.getProperty("simulation-distance")?.trim()?.toIntOrNull() ?: 8,
                syncChunkWrites = props.getProperty("sync-chunk-writes")?.trim()?.toBooleanStrictOrNull() ?: true,
                networkCompressionThreshold = props.getProperty("network-compression-threshold")?.trim()?.toIntOrNull() ?: 256,
                maxTickTime = props.getProperty("max-tick-time")?.trim()?.toLongOrNull() ?: 60000L,
                enableStatus = props.getProperty("enable-status")?.trim()?.toBooleanStrictOrNull() ?: true,
                resourcePack = props.getProperty("resource-pack")?.trim() ?: "",
                resourcePackPrompt = props.getProperty("resource-pack-prompt")?.trim() ?: "",
                requireResourcePack = props.getProperty("require-resource-pack")?.trim()?.toBooleanStrictOrNull() ?: false,
                forceGamemode = props.getProperty("force-gamemode")?.trim()?.toBooleanStrictOrNull() ?: false,
                customProperties = custom
            )
        }
    }
}
