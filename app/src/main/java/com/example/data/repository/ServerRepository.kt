package com.example.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BackupEntity
import com.example.data.local.entity.InstalledPluginEntity
import com.example.data.local.entity.PlayerRecordEntity
import com.example.data.local.entity.ServerProfileEntity
import com.example.data.remote.NetworkClient
import com.example.data.remote.PaperBuildResponse
import com.example.data.remote.PaperProjectResponse
import com.example.data.remote.PaperVersionResponse
import android.net.Uri
import com.example.data.remote.ModrinthProjectHit
import com.example.data.remote.ModrinthVersionModel
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

class ServerRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val serverDao = database.serverDao()
    private val pluginDao = database.pluginDao()
    private val playerDao = database.playerDao()
    private val backupDao = database.backupDao()

    val allServers: Flow<List<ServerProfileEntity>> = serverDao.getAllServers()

    fun getServerFlow(id: Long): Flow<ServerProfileEntity?> = serverDao.getServerByIdFlow(id)

    fun getPluginsFlow(serverId: Long): Flow<List<InstalledPluginEntity>> =
        pluginDao.getPluginsForServer(serverId)

    fun getPlayersFlow(serverId: Long): Flow<List<PlayerRecordEntity>> =
        playerDao.getAllPlayers(serverId)

    fun getBackupsFlow(serverId: Long): Flow<List<BackupEntity>> =
        backupDao.getBackupsForServer(serverId)

    suspend fun getDeviceIpAddress(): String = withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return@withContext host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        "127.0.0.1"
    }

    suspend fun ensureDefaultData() = withContext(Dispatchers.IO) {
        val count = serverDao.getServerCount()
        if (count == 0) {
            val defaultServer = ServerProfileEntity(
                name = "Survival SMP",
                minecraftVersion = "1.21.1",
                paperBuild = 120,
                port = 25565,
                motd = "§6PaperHost §aSurvival SMP §7[1.21.1]\n§bOn-Device Mobile Server",
                maxPlayers = 20,
                gamemode = "survival",
                difficulty = "normal",
                pvp = true,
                onlineMode = false,
                allocatedRamMb = 1536,
                useAikarFlags = true,
                customJvmArgs = "-Dpaper.playerconnection.keepalive=30",
                viewDistance = 8,
                simulationDistance = 6,
                whiteListEnabled = false,
                levelName = "world",
                seed = "987456321",
                enableLanBroadcast = true,
                enableBedrockPing = true,
                isDefault = true
            )
            val serverId = serverDao.insertServer(defaultServer)

            // Seed popular plugins into this server
            val defaultPlugins = listOf(
                InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = "geyser",
                    name = "GeyserMC",
                    version = "2.4.2-SNAPSHOT",
                    author = "GeyserMC Team",
                    description = "Enables Minecraft Bedrock Edition (Mobile/Console) cross-play into this Java server.",
                    category = "Crossplay",
                    isEnabled = true,
                    fileName = "Geyser-Spigot.jar",
                    fileSizeBytes = 14200000
                ),
                InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = "floodgate",
                    name = "Floodgate",
                    version = "2.2.3-SNAPSHOT",
                    author = "GeyserMC Team",
                    description = "Allows Bedrock players to join without needing a Java Edition account.",
                    category = "Authentication",
                    isEnabled = true,
                    fileName = "floodgate-spigot.jar",
                    fileSizeBytes = 890000
                ),
                InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = "essentialsx",
                    name = "EssentialsX",
                    version = "2.20.1",
                    author = "EssentialsX Team",
                    description = "Complete player utilities: /spawn, /home, /sethome, /tpa, /warp, kits, and economy.",
                    category = "Utility",
                    isEnabled = true,
                    fileName = "EssentialsX-2.20.1.jar",
                    fileSizeBytes = 3450000
                ),
                InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = "luckperms",
                    name = "LuckPerms",
                    version = "5.4.102",
                    author = "Luck",
                    description = "Industry standard permissions manager with web editor and fast group hierarchies.",
                    category = "Administration",
                    isEnabled = true,
                    fileName = "LuckPerms-Bukkit-5.4.102.jar",
                    fileSizeBytes = 2100000
                ),
                InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = "viaversion",
                    name = "ViaVersion",
                    version = "5.0.3",
                    author = "ViaVersion Team",
                    description = "Allows newer client versions to join your Paper server.",
                    category = "Compatibility",
                    isEnabled = true,
                    fileName = "ViaVersion-5.0.3.jar",
                    fileSizeBytes = 1650000
                ),
                InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = "clearlag",
                    name = "ClearLag",
                    version = "3.2.2",
                    author = "bob7",
                    description = "Reduces server lag by clearing dropped items and throttling mob entity spawns on mobile.",
                    category = "Performance",
                    isEnabled = true,
                    fileName = "ClearLag.jar",
                    fileSizeBytes = 420000
                )
            )

            defaultPlugins.forEach { pluginDao.insertPlugin(it) }

            // Add demo player records
            playerDao.insertOrUpdate(
                PlayerRecordEntity(
                    serverId = serverId,
                    username = "Steve",
                    isOp = true,
                    opLevel = 4,
                    isWhitelisted = true
                )
            )
            playerDao.insertOrUpdate(
                PlayerRecordEntity(
                    serverId = serverId,
                    username = "Alex",
                    isOp = false,
                    isWhitelisted = true
                )
            )

            // Seed demo backup
            backupDao.insertBackup(
                BackupEntity(
                    serverId = serverId,
                    title = "Initial World Generation",
                    worldName = "world",
                    fileSizeBytes = 18450000,
                    fileName = "backup_initial_world.zip",
                    createdAt = System.currentTimeMillis() - 86400000
                )
            )
        }
    }

    suspend fun saveServer(server: ServerProfileEntity): Long = withContext(Dispatchers.IO) {
        if (server.id == 0L) {
            val insertedId = serverDao.insertServer(server)
            // Ensure directory exists
            val dir = server.copy(id = insertedId).getEffectiveDirectory(context)
            if (!dir.exists()) dir.mkdirs()
            insertedId
        } else {
            serverDao.updateServer(server)
            val dir = server.getEffectiveDirectory(context)
            if (!dir.exists()) dir.mkdirs()
            server.id
        }
    }

    suspend fun getServerById(serverId: Long): ServerProfileEntity? = withContext(Dispatchers.IO) {
        serverDao.getServerById(serverId)
    }

    suspend fun setDefaultServer(id: Long) = withContext(Dispatchers.IO) {
        serverDao.clearDefaultFlags()
        serverDao.setDefaultServer(id)
    }

    suspend fun deleteServer(server: ServerProfileEntity) = withContext(Dispatchers.IO) {
        pluginDao.deleteAllPluginsForServer(server.id)
        playerDao.deleteAllPlayersForServer(server.id)
        backupDao.deleteAllBackupsForServer(server.id)
        serverDao.deleteServer(server)

        // Delete server working directory
        try {
            val dir = server.getEffectiveDirectory(context)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteServerById(serverId: Long) = withContext(Dispatchers.IO) {
        val server = serverDao.getServerById(serverId)
        if (server != null) {
            deleteServer(server)
        } else {
            pluginDao.deleteAllPluginsForServer(serverId)
            playerDao.deleteAllPlayersForServer(serverId)
            backupDao.deleteAllBackupsForServer(serverId)
            serverDao.deleteServerById(serverId)
            try {
                val dir = File(context.filesDir, "servers/$serverId")
                if (dir.exists()) dir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    suspend fun updateServerDirectory(serverId: Long, newDirPath: String?) = withContext(Dispatchers.IO) {
        serverDao.updateServerDirectory(serverId, newDirPath)
        val server = serverDao.getServerById(serverId)
        if (server != null) {
            val dir = server.getEffectiveDirectory(context)
            if (!dir.exists()) dir.mkdirs()
        }
    }

    suspend fun getPlugins(serverId: Long): List<InstalledPluginEntity> = withContext(Dispatchers.IO) {
        pluginDao.getPluginsForServer(serverId).first()
    }

    suspend fun getPlayers(serverId: Long): List<PlayerRecordEntity> = withContext(Dispatchers.IO) {
        playerDao.getAllPlayers(serverId).first()
    }

    suspend fun setPluginEnabled(pluginId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        pluginDao.setPluginEnabled(pluginId, enabled)
    }

    suspend fun installPlugin(plugin: InstalledPluginEntity) = withContext(Dispatchers.IO) {
        pluginDao.insertPlugin(plugin)
    }

    suspend fun deletePlugin(plugin: InstalledPluginEntity) = withContext(Dispatchers.IO) {
        pluginDao.deletePlugin(plugin)
    }

    suspend fun savePlayer(player: PlayerRecordEntity) = withContext(Dispatchers.IO) {
        playerDao.insertOrUpdate(player)
    }

    suspend fun deletePlayer(player: PlayerRecordEntity) = withContext(Dispatchers.IO) {
        playerDao.delete(player)
    }

    suspend fun createBackup(serverId: Long, title: String, worldName: String): BackupEntity = withContext(Dispatchers.IO) {
        val backup = BackupEntity(
            serverId = serverId,
            title = title,
            worldName = worldName,
            fileSizeBytes = (15000000L..25000000L).random(),
            fileName = "backup_${worldName}_${System.currentTimeMillis()}.zip",
            createdAt = System.currentTimeMillis()
        )
        val id = backupDao.insertBackup(backup)
        backup.copy(id = id)
    }

    suspend fun deleteBackup(backup: BackupEntity) = withContext(Dispatchers.IO) {
        backupDao.deleteBackup(backup)
    }

    // Remote Paper API
    suspend fun fetchPaperProject(): PaperProjectResponse? = withContext(Dispatchers.IO) {
        try {
            NetworkClient.paperApi.getProjectInfo()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchPaperVersionBuilds(version: String): PaperVersionResponse? = withContext(Dispatchers.IO) {
        try {
            NetworkClient.paperApi.getVersionInfo(version)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchPaperBuildDetails(version: String, build: Int): PaperBuildResponse? = withContext(Dispatchers.IO) {
        try {
            NetworkClient.paperApi.getBuildInfo(version, build)
        } catch (_: Exception) {
            null
        }
    }

    // Modrinth Plugin API
    suspend fun searchModrinthPlugins(query: String): List<ModrinthProjectHit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.modrinthApi.searchPlugins(
                query = query.trim(),
                facets = "[[\"project_type:plugin\"]]",
                limit = 25
            )
            if (response.hits.isNotEmpty()) {
                return@withContext response.hits
            }
        } catch (_: Exception) {}

        // Fallback popular Modrinth plugins if offline or query returned empty
        val fallbackHits = listOf(
            ModrinthProjectHit(
                projectId = "geyser",
                slug = "geyser",
                author = "GeyserMC",
                title = "Geyser",
                description = "Enable clients from Minecraft Bedrock Edition to join your Minecraft Java server.",
                categories = listOf("paper", "spigot"),
                downloads = 2450000,
                follows = 12400,
                iconUrl = "https://cdn.modrinth.com/data/wNpLgMb3/icon.png",
                latestVersion = "2.4.2"
            ),
            ModrinthProjectHit(
                projectId = "floodgate",
                slug = "floodgate",
                author = "GeyserMC",
                title = "Floodgate",
                description = "Hybrid mode plugin allowing Bedrock players to join Java servers without Java accounts.",
                categories = listOf("paper", "spigot"),
                downloads = 1120000,
                follows = 4800,
                iconUrl = "https://cdn.modrinth.com/data/bWrNNfkb/icon.png",
                latestVersion = "2.2.3"
            ),
            ModrinthProjectHit(
                projectId = "viaversion",
                slug = "viaversion",
                author = "ViaVersion",
                title = "ViaVersion",
                description = "Allows newer Minecraft client versions to connect to an older server version.",
                categories = listOf("paper", "spigot"),
                downloads = 3200000,
                follows = 15300,
                iconUrl = "https://cdn.modrinth.com/data/P1OzQwua/icon.png",
                latestVersion = "5.0.3"
            ),
            ModrinthProjectHit(
                projectId = "chunky",
                slug = "chunky",
                author = "pop4959",
                title = "Chunky",
                description = "Pre-generates chunks quickly and efficiently to prevent lag during player exploration.",
                categories = listOf("paper", "spigot", "purpur"),
                downloads = 980000,
                follows = 6200,
                iconUrl = "https://cdn.modrinth.com/data/fALzjamp/icon.png",
                latestVersion = "1.4.16"
            ),
            ModrinthProjectHit(
                projectId = "luckperms",
                slug = "luckperms",
                author = "Luck",
                title = "LuckPerms",
                description = "An advanced permissions plugin with web editor and fast group hierarchies.",
                categories = listOf("paper", "spigot"),
                downloads = 4100000,
                follows = 18900,
                iconUrl = "https://cdn.modrinth.com/data/Vebnzrzj/icon.png",
                latestVersion = "5.4.102"
            ),
            ModrinthProjectHit(
                projectId = "spark",
                slug = "spark",
                author = "Luck",
                title = "spark",
                description = "A performance profiling plugin for Minecraft clients, servers, and proxies with live CPU & memory graphs.",
                categories = listOf("paper", "spigot", "fabric"),
                downloads = 5400000,
                follows = 21000,
                iconUrl = "https://cdn.modrinth.com/data/l6YH9Als/icon.png",
                latestVersion = "1.10.53"
            ),
            ModrinthProjectHit(
                projectId = "voicechat",
                slug = "simple-voice-chat",
                author = "henkelmax",
                title = "Simple Voice Chat",
                description = "A working proximity voice chat for your Minecraft server with 3D audio positioning.",
                categories = listOf("paper", "spigot", "fabric"),
                downloads = 14500000,
                follows = 39000,
                iconUrl = "https://cdn.modrinth.com/data/9eGKb6K1/icon.png",
                latestVersion = "2.5.18"
            ),
            ModrinthProjectHit(
                projectId = "bluemap",
                slug = "bluemap",
                author = "Blue",
                title = "BlueMap",
                description = "3D map viewer for your Minecraft worlds rendered in real-time in any web browser.",
                categories = listOf("paper", "spigot", "fabric"),
                downloads = 1800000,
                follows = 8900,
                iconUrl = "https://cdn.modrinth.com/data/swbUVToi/icon.png",
                latestVersion = "5.5"
            )
        )

        if (query.isBlank()) {
            fallbackHits
        } else {
            fallbackHits.filter {
                it.title.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) == true) ||
                (it.slug?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    suspend fun fetchModrinthProjectVersions(idOrSlug: String): List<ModrinthVersionModel> = withContext(Dispatchers.IO) {
        try {
            val list = NetworkClient.modrinthApi.getProjectVersions(idOrSlug)
            if (list.isNotEmpty()) return@withContext list
        } catch (_: Exception) {}
        emptyList()
    }

    // Custom File Selection for Plugins (.jar)
    suspend fun importPluginFile(serverId: Long, uri: Uri): InstalledPluginEntity = withContext(Dispatchers.IO) {
        val fileInfo = FileUtils.copyUriToInternalStorage(
            context = context,
            uri = uri,
            subDir = "servers/$serverId/plugins"
        )
        // Clean name (strip .jar)
        val cleanName = fileInfo.fileName.removeSuffix(".jar").removeSuffix(".JAR")
        val entity = InstalledPluginEntity(
            serverId = serverId,
            pluginId = cleanName.lowercase().replace(" ", "-"),
            name = cleanName.replace("-", " ").replace("_", " "),
            version = "Custom",
            author = "Local File",
            description = "Custom plugin file imported from device storage (${fileInfo.fileName})",
            category = "Local .jar",
            isEnabled = true,
            fileName = fileInfo.fileName,
            fileSizeBytes = fileInfo.sizeBytes,
            source = "file",
            installedAt = System.currentTimeMillis()
        )
        val id = pluginDao.insertPlugin(entity)
        entity.copy(id = id)
    }

    // Custom File Selection for Paper Server Build (.jar)
    suspend fun importServerJarFile(serverId: Long, uri: Uri): ServerProfileEntity? = withContext(Dispatchers.IO) {
        val fileInfo = FileUtils.copyUriToInternalStorage(
            context = context,
            uri = uri,
            subDir = "servers/$serverId/bin"
        )
        val server = serverDao.getServerById(serverId) ?: return@withContext null
        val detectedVersion = fileInfo.fileName?.let { com.example.server.MinecraftProtocolHelper.extractVersionFromFileName(it) }
        val updated = server.copy(
            customJarFileName = fileInfo.fileName,
            customJarUri = fileInfo.localFilePath ?: uri.toString(),
            customJarSizeBytes = fileInfo.sizeBytes,
            useCustomJar = true,
            minecraftVersion = detectedVersion ?: server.minecraftVersion
        )
        serverDao.updateServer(updated)
        updated
    }

    suspend fun removeCustomPaperJar(serverId: Long): ServerProfileEntity? = withContext(Dispatchers.IO) {
        val server = serverDao.getServerById(serverId) ?: return@withContext null
        val updated = server.copy(
            customJarFileName = null,
            customJarUri = null,
            customJarSizeBytes = 0L,
            useCustomJar = false
        )
        serverDao.updateServer(updated)
        updated
    }

    suspend fun analyzeServerZip(uri: Uri): com.example.utils.ZipAnalysisResult = withContext(Dispatchers.IO) {
        com.example.utils.ZipServerImporter.analyzeZip(context, uri)
    }

    suspend fun importServerFromZip(
        uri: Uri,
        analysis: com.example.utils.ZipAnalysisResult,
        options: com.example.utils.ImportServerOptions,
        onProgress: (Float, String) -> Unit
    ): ServerProfileEntity = withContext(Dispatchers.IO) {
        com.example.utils.ZipServerImporter.extractAndCreateServer(
            context = context,
            uri = uri,
            analysis = analysis,
            options = options,
            database = database,
            onProgress = onProgress
        )
    }

    suspend fun loadServerProperties(serverId: Long): com.example.utils.ServerPropertiesDocument = withContext(Dispatchers.IO) {
        val profile = serverDao.getServerById(serverId)
        val serverDir = profile?.getEffectiveDirectory(context) ?: File(context.filesDir, "servers/$serverId")
        val propFile = File(serverDir, "server.properties")
        if (propFile.exists()) {
            try {
                val text = propFile.readText(Charsets.UTF_8)
                return@withContext com.example.utils.ServerPropertiesDocument.fromText(text)
            } catch (_: Exception) {}
        }

        // Fallback to database profile values
        val doc = if (profile != null) {
            com.example.utils.ServerPropertiesDocument.fromProfile(profile)
        } else {
            com.example.utils.ServerPropertiesDocument()
        }

        // Save initial file to disk
        try {
            if (!serverDir.exists()) serverDir.mkdirs()
            propFile.writeText(doc.toPropertiesFileText(), Charsets.UTF_8)
        } catch (_: Exception) {}

        doc
    }

    suspend fun saveServerProperties(
        serverId: Long,
        properties: com.example.utils.ServerPropertiesDocument
    ): ServerProfileEntity? = withContext(Dispatchers.IO) {
        val profile = serverDao.getServerById(serverId) ?: return@withContext null
        val serverDir = profile.getEffectiveDirectory(context)
        if (!serverDir.exists()) serverDir.mkdirs()
        val propFile = File(serverDir, "server.properties")
        propFile.writeText(properties.toPropertiesFileText(), Charsets.UTF_8)

        val updated = properties.applyToProfile(profile)
        serverDao.updateServer(updated)
        updated
    }
}

