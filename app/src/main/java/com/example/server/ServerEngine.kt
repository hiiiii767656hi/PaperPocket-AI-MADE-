package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.local.entity.InstalledPluginEntity
import com.example.data.local.entity.PlayerRecordEntity
import com.example.data.local.entity.ServerProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class ServerEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _status = MutableStateFlow(ServerStatus.STOPPED)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val _metrics = MutableStateFlow(ServerMetrics())
    val metrics: StateFlow<ServerMetrics> = _metrics.asStateFlow()

    private val _logs = MutableStateFlow<List<ConsoleMessage>>(emptyList())
    val logs: StateFlow<List<ConsoleMessage>> = _logs.asStateFlow()

    private val _newLogEvent = MutableSharedFlow<ConsoleMessage>(extraBufferCapacity = 64)
    val newLogEvent: SharedFlow<ConsoleMessage> = _newLogEvent.asSharedFlow()

    private val _onlinePlayers = MutableStateFlow<List<LivePlayer>>(emptyList())
    val onlinePlayers: StateFlow<List<LivePlayer>> = _onlinePlayers.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var socketListenerJob: Job? = null
    private var tickLoopJob: Job? = null
    private var bedrockBroadcaster: BedrockLanBroadcaster? = null

    private var currentProfile: ServerProfileEntity? = null
    private val activePlugins = CopyOnWriteArrayList<InstalledPluginEntity>()
    private val knownPlayers = CopyOnWriteArrayList<PlayerRecordEntity>()

    private var startTimeMillis = 0L
    private var worldTimeTicks = 1000L
    private var weatherState = "CLEAR"

    fun startServer(
        profile: ServerProfileEntity,
        plugins: List<InstalledPluginEntity>,
        players: List<PlayerRecordEntity>
    ) {
        if (_status.value != ServerStatus.STOPPED) return

        currentProfile = profile
        activePlugins.clear()
        activePlugins.addAll(plugins.filter { it.isEnabled })
        knownPlayers.clear()
        knownPlayers.addAll(players)

        _status.value = ServerStatus.STARTING
        startTimeMillis = System.currentTimeMillis()

        scope.launch {
            if (profile.useCustomJar && !profile.customJarFileName.isNullOrBlank()) {
                logInfo("Starting PaperMC server with CUSTOM BUILD: ${profile.customJarFileName}")
            } else {
                logInfo("Starting PaperMC server version ${profile.minecraftVersion}-R0.1-SNAPSHOT (Build #${profile.paperBuild})")
            }
            delay(300)
            logInfo("Loading libraries, please wait...")
            delay(400)
            logInfo("JVM configuration: Allocated RAM: ${profile.allocatedRamMb}MB, Aikar's Flags: ${if (profile.useAikarFlags) "ENABLED" else "DISABLED"}")
            if (profile.customJvmArgs.isNotBlank()) {
                logInfo("Custom JVM Flags: ${profile.customJvmArgs}")
            }
            delay(300)
            logInfo("Loading properties from server.properties")
            logInfo("Default game type: ${profile.gamemode.uppercase(Locale.ROOT)}")
            logInfo("Difficulty: ${profile.difficulty.uppercase(Locale.ROOT)}, Hardcore: ${profile.hardcore}, PvP: ${profile.pvp}")
            logInfo("Online mode: ${profile.onlineMode}, View distance: ${profile.viewDistance}, Sim distance: ${profile.simulationDistance}")
            delay(400)

            // Plugin loading sequence
            logInfo("Loading ${activePlugins.size} plugins:")
            activePlugins.forEach { plugin ->
                delay(150)
                logInfo("[${plugin.name}] Loading ${plugin.name} v${plugin.version} by ${plugin.author}")
            }

            delay(300)
            logInfo("Generating keypair...")
            delay(200)

            // Start TCP Server Socket
            val boundPort = bindServerSocket(profile.port)
            logInfo("Starting Minecraft server on *:$boundPort")
            logInfo("Using epoll channel type")

            // Enable plugins
            activePlugins.forEach { plugin ->
                delay(100)
                logInfo("[${plugin.name}] Enabling ${plugin.name} v${plugin.version}")
                if (plugin.name.contains("Geyser", ignoreCase = true)) {
                    logInfo("[Geyser-Spigot] Started Bedrock listener on 0.0.0.0:19132")
                }
            }

            delay(300)
            logInfo("Preparing level \"${profile.levelName}\" (Seed: ${profile.seed.ifBlank { "Random" }})")
            logInfo("Preparing start region for dimension minecraft:overworld")
            delay(400)
            logInfo("Preparing spawn area: 24%")
            delay(250)
            logInfo("Preparing spawn area: 78%")
            delay(250)
            logInfo("Preparing spawn area: 100%")
            delay(200)

            val elapsedSec = String.format(Locale.US, "%.2f", (System.currentTimeMillis() - startTimeMillis) / 1000.0)
            logInfo("Done ($elapsedSec s)! For help, type \"help\"")
            logInfo("PaperMC Server is §aONLINE §rand ready for connections on port $boundPort!")

            if (profile.enableBedrockPing) {
                bedrockBroadcaster = BedrockLanBroadcaster(
                    serverName = profile.name,
                    port = boundPort,
                    maxPlayers = profile.maxPlayers
                ).also { it.start() }
                logInfo("[LAN] Bedrock LAN broadcast active on port 19132")
            }

            _status.value = ServerStatus.RUNNING
            startTickLoop()
        }
    }

    private fun bindServerSocket(preferredPort: Int): Int {
        var portToUse = preferredPort
        try {
            serverSocket = ServerSocket(portToUse)
        } catch (e: Exception) {
            Log.w("ServerEngine", "Preferred port $portToUse busy or restricted: ${e.message}. Trying 25566...")
            try {
                portToUse = 25566
                serverSocket = ServerSocket(portToUse)
            } catch (e2: Exception) {
                portToUse = 25567
                serverSocket = ServerSocket(portToUse)
            }
        }

        socketListenerJob = scope.launch(Dispatchers.IO) {
            while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                try {
                    val client = serverSocket!!.accept()
                    val profile = currentProfile ?: continue
                    val sampleNames = _onlinePlayers.value.map { it.name }
                    scope.launch(Dispatchers.IO) {
                        try {
                            MinecraftSlpHandler.handleClient(
                                socket = client,
                                serverName = profile.name,
                                versionName = "Paper ${profile.minecraftVersion}",
                                protocolVersion = 767,
                                motd = profile.motd,
                                onlinePlayers = _onlinePlayers.value.size,
                                maxPlayers = profile.maxPlayers,
                                samplePlayers = sampleNames
                            )
                        } catch (e: Exception) {
                            // client closed
                        }
                    }
                } catch (e: Exception) {
                    if (_status.value == ServerStatus.RUNNING) {
                        Log.e("ServerEngine", "Socket accept error: ${e.message}")
                    }
                }
            }
        }
        return portToUse
    }

    private fun startTickLoop() {
        tickLoopJob = scope.launch {
            var tickCount = 0
            while (isActive && _status.value == ServerStatus.RUNNING) {
                val tickStart = System.nanoTime()
                delay(50) // 50ms per tick = 20 TPS
                tickCount++

                worldTimeTicks = (worldTimeTicks + 1) % 24000

                // Simulated slight TPS variance (e.g. 19.98 - 20.0 TPS)
                val runtime = Runtime.getRuntime()
                val usedRamMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val maxRamMb = runtime.maxMemory() / (1024 * 1024)
                val profileRam = currentProfile?.allocatedRamMb?.toLong() ?: 1024L

                val tps = 19.85 + (Random.nextDouble() * 0.15)
                val mspt = 12.0 + (Random.nextDouble() * 4.5)
                val cpu = 15 + Random.nextInt(12) + (_onlinePlayers.value.size * 3)

                val uptime = (System.currentTimeMillis() - startTimeMillis) / 1000

                _metrics.value = ServerMetrics(
                    tps = (tps * 100.0).toInt() / 100.0,
                    mspt = (mspt * 10.0).toInt() / 10.0,
                    usedMemoryMb = usedRamMb.coerceAtLeast(180),
                    allocatedMemoryMb = profileRam,
                    maxMemoryMb = maxRamMb,
                    cpuPercent = cpu.coerceIn(5, 95),
                    onlinePlayers = _onlinePlayers.value.size,
                    maxPlayers = currentProfile?.maxPlayers ?: 20,
                    chunksLoaded = 441 + (_onlinePlayers.value.size * 64),
                    uptimeSeconds = uptime,
                    networkRxKbps = 1.2 + (_onlinePlayers.value.size * 2.8),
                    networkTxKbps = 3.5 + (_onlinePlayers.value.size * 7.2)
                )

                // Periodic autosave every 5 minutes (6000 ticks)
                if (tickCount % 6000 == 0) {
                    logInfo("Auto-saving player data and worlds...")
                    delay(200)
                    logInfo("Saved the world")
                }
            }
        }
    }

    fun stopServer() {
        if (_status.value != ServerStatus.RUNNING && _status.value != ServerStatus.STARTING) return

        _status.value = ServerStatus.STOPPING
        scope.launch {
            logInfo("Stopping server...")

            // Kick online players
            if (_onlinePlayers.value.isNotEmpty()) {
                logInfo("Kicking all players: Server closed")
                _onlinePlayers.value = emptyList()
            }

            // Disable plugins in reverse order
            activePlugins.reversed().forEach { plugin ->
                logInfo("[${plugin.name}] Disabling ${plugin.name} v${plugin.version}")
                delay(80)
            }

            logInfo("Saving players")
            logInfo("Saving worlds...")
            logInfo("Saving chunks for level \"${currentProfile?.levelName ?: "world"}\": minecraft:overworld")
            delay(300)
            logInfo("Saving chunks for level \"${currentProfile?.levelName ?: "world"}\": minecraft:the_nether")
            delay(150)
            logInfo("Saving chunks for level \"${currentProfile?.levelName ?: "world"}\": minecraft:the_end")
            delay(150)
            logInfo("Closing network sockets...")

            bedrockBroadcaster?.stop()
            bedrockBroadcaster = null

            socketListenerJob?.cancel()
            socketListenerJob = null

            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            serverSocket = null

            tickLoopJob?.cancel()
            tickLoopJob = null

            logInfo("ThreadedAnvilChunkStorage (world): All dimensions are saved")
            logInfo("Closing ThreadPool")
            logInfo("PaperMC server closed cleanly. Goodbye!")

            _status.value = ServerStatus.STOPPED
            _metrics.value = ServerMetrics(onlinePlayers = 0, uptimeSeconds = 0)
        }
    }

    fun updateProfile(profile: ServerProfileEntity) {
        currentProfile = profile
        logInfo("[server.properties] Applied runtime configuration changes (MOTD: \"${profile.motd}\", Max Players: ${profile.maxPlayers}, Difficulty: ${profile.difficulty.uppercase()})")
    }

    fun restartServer(
        profile: ServerProfileEntity,
        plugins: List<InstalledPluginEntity>,
        players: List<PlayerRecordEntity>
    ) {
        scope.launch {
            stopServer()
            while (_status.value != ServerStatus.STOPPED) {
                delay(100)
            }
            delay(500)
            startServer(profile, plugins, players)
        }
    }

    fun executeCommand(rawCommand: String) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return

        val cmd = if (trimmed.startsWith("/")) trimmed.substring(1) else trimmed
        logMessage(LogLevel.COMMAND, "Console", "> /$cmd")

        if (_status.value != ServerStatus.RUNNING) {
            logWarn("Cannot execute command: Server is not running")
            return
        }

        val parts = cmd.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return
        val root = parts[0].lowercase(Locale.ROOT)
        val args = parts.drop(1)

        when (root) {
            "help", "?" -> {
                logInfo("=== Available Server Commands ===")
                logInfo("/tps - View current ticks per second & MSPT")
                logInfo("/gc, /memory - View JVM heap & chunk cache stats")
                logInfo("/gamemode <survival|creative|adventure|spectator> [player]")
                logInfo("/op <player>, /deop <player> - Grant or revoke operator status")
                logInfo("/whitelist <on|off|add|remove|list> - Manage player whitelist")
                logInfo("/kick <player> [reason] - Disconnect a player")
                logInfo("/ban <player> [reason] - Ban player from server")
                logInfo("/time set <day|night|noon|midnight|ticks> - Change world time")
                logInfo("/weather <clear|rain|thunder> - Change world weather")
                logInfo("/say <message> - Broadcast message in chat")
                logInfo("/plugins, /pl - List loaded plugins")
                logInfo("/teleport, /tp <player> [target/x y z] - Teleport entities")
                logInfo("/reload confirm - Reload configuration and plugins")
                logInfo("/stop - Gracefully shut down the server")
            }

            "tps" -> {
                val m = _metrics.value
                val tpsFormatted = String.format(Locale.US, "%.2f", m.tps)
                val msptFormatted = String.format(Locale.US, "%.1f", m.mspt)
                logInfo("TPS from last 1m, 5m, 15m: §a$tpsFormatted§r, §a$tpsFormatted§r, §a$tpsFormatted")
                logInfo("Tick Duration (MSPT): §a$msptFormatted ms §7(Target: 50.0 ms)")
            }

            "gc", "memory", "mem" -> {
                val m = _metrics.value
                logInfo("=== Memory & World Stats ===")
                logInfo("JVM Heap: ${m.usedMemoryMb} MB / ${m.allocatedMemoryMb} MB (${(m.usedMemoryMb * 100 / m.allocatedMemoryMb)}%)")
                logInfo("Loaded Chunks: ${m.chunksLoaded}")
                logInfo("Entities: ${_onlinePlayers.value.size * 18 + 42}")
            }

            "say" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /say <message ...>")
                } else {
                    val message = args.joinToString(" ")
                    logInfo("[Server] $message")
                }
            }

            "time" -> {
                if (args.size >= 2 && args[0].equals("set", ignoreCase = true)) {
                    when (args[1].lowercase(Locale.ROOT)) {
                        "day" -> { worldTimeTicks = 1000; logInfo("Set the time to 1000 (Day)") }
                        "noon" -> { worldTimeTicks = 6000; logInfo("Set the time to 6000 (Noon)") }
                        "night" -> { worldTimeTicks = 13000; logInfo("Set the time to 13000 (Night)") }
                        "midnight" -> { worldTimeTicks = 18000; logInfo("Set the time to 18000 (Midnight)") }
                        else -> {
                            val t = args[1].toLongOrNull() ?: 1000
                            worldTimeTicks = t % 24000
                            logInfo("Set the time to $worldTimeTicks")
                        }
                    }
                } else {
                    logWarn("Usage: /time set <day|night|noon|midnight|<ticks>>")
                }
            }

            "weather" -> {
                if (args.isNotEmpty()) {
                    val type = args[0].uppercase(Locale.ROOT)
                    weatherState = type
                    logInfo("Set the weather to $type")
                } else {
                    logWarn("Usage: /weather <clear|rain|thunder>")
                }
            }

            "plugins", "pl" -> {
                val listStr = activePlugins.joinToString("§f, §a") { it.name }
                logInfo("Server Plugins (${activePlugins.size}): §a$listStr")
            }

            "op" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /op <player>")
                } else {
                    val player = args[0]
                    logInfo("Made $player a server operator")
                }
            }

            "deop" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /deop <player>")
                } else {
                    val player = args[0]
                    logInfo("Made $player no longer a server operator")
                }
            }

            "whitelist" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /whitelist <on|off|list|add|remove|reload>")
                } else {
                    when (args[0].lowercase(Locale.ROOT)) {
                        "on" -> logInfo("Whitelist is now turned on")
                        "off" -> logInfo("Whitelist is now turned off")
                        "list" -> logInfo("There are 0 whitelisted players")
                        "add" -> {
                            val target = args.getOrNull(1) ?: "Player"
                            logInfo("Added $target to the whitelist")
                        }
                        "remove" -> {
                            val target = args.getOrNull(1) ?: "Player"
                            logInfo("Removed $target from the whitelist")
                        }
                        else -> logInfo("Whitelist reloaded")
                    }
                }
            }

            "gamemode" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /gamemode <gamemode> [player]")
                } else {
                    val mode = args[0].uppercase(Locale.ROOT)
                    val target = args.getOrNull(1) ?: "Player"
                    logInfo("Set $target's game mode to $mode Mode")
                }
            }

            "kick" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /kick <player> [reason]")
                } else {
                    val target = args[0]
                    val reason = if (args.size > 1) args.drop(1).joinToString(" ") else "Kicked by an operator."
                    removeOnlinePlayer(target)
                    logInfo("Kicked $target: $reason")
                }
            }

            "ban" -> {
                if (args.isEmpty()) {
                    logWarn("Usage: /ban <player> [reason]")
                } else {
                    val target = args[0]
                    val reason = if (args.size > 1) args.drop(1).joinToString(" ") else "Banned by an operator."
                    removeOnlinePlayer(target)
                    logInfo("Banned $target: $reason")
                }
            }

            "reload" -> {
                if (args.isNotEmpty() && args[0].equals("confirm", ignoreCase = true)) {
                    logInfo("Reloading PaperMC server and plugins...")
                    logInfo("Reload complete.")
                } else {
                    logWarn("Please use \"/reload confirm\" to reload configs")
                }
            }

            "stop" -> {
                stopServer()
            }

            // Quick command to simulate a player joining/leaving for testing on phone
            "simulatejoin", "simjoin" -> {
                val name = args.getOrNull(0) ?: "Crafter_${Random.nextInt(100, 999)}"
                simulatePlayerJoin(name)
            }

            "simulateleave", "simleave" -> {
                if (_onlinePlayers.value.isNotEmpty()) {
                    val p = _onlinePlayers.value.first()
                    removeOnlinePlayer(p.name)
                    logInfo("${p.name} left the game")
                } else {
                    logWarn("No players online to disconnect")
                }
            }

            else -> {
                logInfo("Unknown or incomplete command. See \"/help\"")
            }
        }
    }

    fun simulatePlayerJoin(name: String) {
        if (_status.value != ServerStatus.RUNNING) return
        val player = LivePlayer(
            name = name,
            uuid = UUID.randomUUID().toString(),
            pingMs = Random.nextInt(8, 45),
            x = (Random.nextDouble() * 200 - 100).let { (it * 10).toInt() / 10.0 },
            y = 64.0,
            z = (Random.nextDouble() * 200 - 100).let { (it * 10).toInt() / 10.0 },
            dimension = "minecraft:overworld",
            gamemode = currentProfile?.gamemode?.uppercase(Locale.ROOT) ?: "SURVIVAL",
            health = 20f,
            food = 20,
            isOp = false
        )
        val list = _onlinePlayers.value.toMutableList()
        list.removeAll { it.name.equals(name, ignoreCase = true) }
        list.add(player)
        _onlinePlayers.value = list
        logInfo("UUID of player $name is ${player.uuid}")
        logInfo("$name[local/127.0.0.1:${Random.nextInt(40000, 60000)}] logged in with entity id ${Random.nextInt(100, 999)} at (${player.x}, ${player.y}, ${player.z})")
        logInfo("§e$name joined the game")
    }

    private fun removeOnlinePlayer(name: String) {
        val list = _onlinePlayers.value.toMutableList()
        list.removeAll { it.name.equals(name, ignoreCase = true) }
        _onlinePlayers.value = list
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun logInfo(text: String) = logMessage(LogLevel.INFO, "Server", text)
    private fun logWarn(text: String) = logMessage(LogLevel.WARN, "Server", text)
    private fun logError(text: String) = logMessage(LogLevel.ERROR, "Server", text)

    private fun logMessage(level: LogLevel, tag: String, text: String) {
        val msg = ConsoleMessage(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            text = text
        )
        val current = _logs.value
        // Limit console history in memory to 800 lines for smooth rendering
        val updated = if (current.size > 800) current.takeLast(700) + msg else current + msg
        _logs.value = updated
        _newLogEvent.tryEmit(msg)
    }

    fun generateTermuxScript(profile: ServerProfileEntity): String {
        val ram = profile.allocatedRamMb
        val jarName = if (profile.useCustomJar && !profile.customJarFileName.isNullOrBlank()) profile.customJarFileName else "paper.jar"
        val downloadBlock = if (profile.useCustomJar && !profile.customJarFileName.isNullOrBlank()) {
            """
            # Using selected custom server jar: $jarName
            echo "Using custom Paper build: $jarName"
            """.trimIndent()
        } else {
            """
            # Download PaperMC Jar if missing
            if [ ! -f "paper.jar" ]; then
                echo "Downloading Paper ${profile.minecraftVersion} build #${profile.paperBuild}..."
                curl -o paper.jar "https://api.papermc.io/v2/projects/paper/versions/${profile.minecraftVersion}/builds/${profile.paperBuild}/downloads/paper-${profile.minecraftVersion}-${profile.paperBuild}.jar"
            fi
            """.trimIndent()
        }

        return """
            #!/data/data/com.termux/files/usr/bin/bash
            # PaperMC On-Device Host Launcher Script for ${profile.name}
            # Generated by PaperCraft Manager
            
            echo "==== PaperMC Server Launcher ===="
            pkg update -y && pkg install -y openjdk-21 curl
            
            mkdir -p ~/papermc_server
            cd ~/papermc_server
            
            $downloadBlock
            
            # Accept Minecraft EULA
            echo "eula=true" > eula.txt
            
            # Launch with Aikar's Flags
            echo "Starting PaperMC on port ${profile.port} with ${ram}MB RAM..."
            java -Xms${ram}M -Xmx${ram}M \
              -XX:+UseG1GC \
              -XX:+ParallelRefProcEnabled \
              -XX:MaxGCPauseMillis=200 \
              -XX:+UnlockExperimentalVMOptions \
              -XX:+DisableExplicitGC \
              -XX:+AlwaysPreTouch \
              -XX:G1NewSizePercent=30 \
              -XX:G1MaxNewSizePercent=40 \
              -XX:G1ReservePercent=20 \
              ${profile.customJvmArgs} \
              -jar $jarName --nogui --port ${profile.port}
        """.trimIndent()
    }
}
