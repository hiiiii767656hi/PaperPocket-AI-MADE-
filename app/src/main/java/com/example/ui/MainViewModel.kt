package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PaperCraftApplication
import com.example.data.local.entity.BackupEntity
import com.example.data.local.entity.InstalledPluginEntity
import com.example.data.local.entity.PlayerRecordEntity
import com.example.data.local.entity.ServerProfileEntity
import com.example.data.remote.ModrinthProjectHit
import com.example.server.ConsoleMessage
import com.example.server.LivePlayer
import com.example.server.LogLevel
import com.example.server.PojavChatMessage
import com.example.server.PojavClientState
import com.example.server.PojavSessionListener
import com.example.server.ServerMetrics
import com.example.server.ServerStatus
import com.example.service.PaperServerService
import com.example.utils.ImportServerOptions
import com.example.utils.ServerPropertiesDocument
import com.example.utils.ZipAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PaperCraftApplication
    private val repo = app.repository
    private val engine = app.serverEngine

    val serverStatus: StateFlow<ServerStatus> = engine.status
    val metrics: StateFlow<ServerMetrics> = engine.metrics
    val logs: StateFlow<List<ConsoleMessage>> = engine.logs
    val onlinePlayers: StateFlow<List<LivePlayer>> = engine.onlinePlayers
    val runningServerId: StateFlow<Long?> = engine.runningServerId
    val runningServer: StateFlow<ServerProfileEntity?> = engine.runningServer

    val allServers: StateFlow<List<ServerProfileEntity>> = repo.allServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedServerId = MutableStateFlow<Long?>(null)
    val selectedServerId: StateFlow<Long?> = _selectedServerId.asStateFlow()

    val currentServer: StateFlow<ServerProfileEntity?> = combine(
        allServers,
        _selectedServerId
    ) { servers, selectedId ->
        if (servers.isEmpty()) null
        else if (selectedId == null) servers.firstOrNull { it.isDefault } ?: servers.firstOrNull()
        else servers.firstOrNull { it.id == selectedId } ?: servers.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPlugins: StateFlow<List<InstalledPluginEntity>> = currentServer.flatMapLatest { s ->
        if (s == null) flowOf(emptyList()) else repo.getPluginsFlow(s.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPlayers: StateFlow<List<PlayerRecordEntity>> = currentServer.flatMapLatest { s ->
        if (s == null) flowOf(emptyList()) else repo.getPlayersFlow(s.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBackups: StateFlow<List<BackupEntity>> = currentServer.flatMapLatest { s ->
        if (s == null) flowOf(emptyList()) else repo.getBackupsFlow(s.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceIp = MutableStateFlow("127.0.0.1")
    val deviceIp: StateFlow<String> = _deviceIp.asStateFlow()

    private val _paperVersions = MutableStateFlow<List<String>>(
        listOf("1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.2", "1.19.4", "1.18.2", "1.16.5")
    )
    val paperVersions: StateFlow<List<String>> = _paperVersions.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    // Modrinth State
    private val _modrinthHits = MutableStateFlow<List<ModrinthProjectHit>>(emptyList())
    val modrinthHits: StateFlow<List<ModrinthProjectHit>> = _modrinthHits.asStateFlow()

    private val _isSearchingModrinth = MutableStateFlow(false)
    val isSearchingModrinth: StateFlow<Boolean> = _isSearchingModrinth.asStateFlow()

    private val _modrinthQuery = MutableStateFlow("")
    val modrinthQuery: StateFlow<String> = _modrinthQuery.asStateFlow()

    // Zip Server Import state
    private val _isAnalyzingZip = MutableStateFlow(false)
    val isAnalyzingZip: StateFlow<Boolean> = _isAnalyzingZip.asStateFlow()

    private val _zipAnalysis = MutableStateFlow<ZipAnalysisResult?>(null)
    val zipAnalysis: StateFlow<ZipAnalysisResult?> = _zipAnalysis.asStateFlow()

    private val _isExtractingZip = MutableStateFlow(false)
    val isExtractingZip: StateFlow<Boolean> = _isExtractingZip.asStateFlow()

    private val _zipExtractionProgress = MutableStateFlow(0f)
    val zipExtractionProgress: StateFlow<Float> = _zipExtractionProgress.asStateFlow()

    private val _zipExtractionStatus = MutableStateFlow("")
    val zipExtractionStatus: StateFlow<String> = _zipExtractionStatus.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    private val _pendingZipUri = MutableStateFlow<Uri?>(null)
    val pendingZipUri: StateFlow<Uri?> = _pendingZipUri.asStateFlow()

    // Server properties editor state
    private val _serverProperties = MutableStateFlow<ServerPropertiesDocument?>(null)
    val serverProperties: StateFlow<ServerPropertiesDocument?> = _serverProperties.asStateFlow()

    private val _isLoadingProperties = MutableStateFlow(false)
    val isLoadingProperties: StateFlow<Boolean> = _isLoadingProperties.asStateFlow()

    private val _isSavingProperties = MutableStateFlow(false)
    val isSavingProperties: StateFlow<Boolean> = _isSavingProperties.asStateFlow()

    init {
        com.example.server.PlayitTunnelManager.init(getApplication())
        viewModelScope.launch {
            _deviceIp.value = repo.getDeviceIpAddress()
            fetchRemotePaperVersions()
            searchModrinth("") // Load initial trending/popular plugins
        }

        viewModelScope.launch {
            currentServer.collect { server ->
                if (server != null) {
                    loadServerProperties(server.id)
                }
            }
        }
    }

    private suspend fun fetchRemotePaperVersions() {
        val proj = repo.fetchPaperProject()
        if (proj != null && proj.versions.isNotEmpty()) {
            _paperVersions.value = proj.versions.reversed()
        }
    }

    fun selectServer(id: Long) {
        _selectedServerId.value = id
        viewModelScope.launch {
            repo.setDefaultServer(id)
        }
    }

    fun startServer(targetServerId: Long? = null) {
        viewModelScope.launch {
            val serverId = targetServerId ?: currentServer.value?.id ?: _selectedServerId.value ?: return@launch
            val server = repo.getServerById(serverId)
                ?: allServers.value.firstOrNull { it.id == serverId }
                ?: currentServer.value ?: return@launch

            // If another server was running or stopping, cleanly wait for it to fully stop first
            if (engine.status.value != ServerStatus.STOPPED) {
                if (engine.runningServerId.value != server.id) {
                    engine.stopServerAwait()
                } else {
                    return@launch
                }
            }

            _selectedServerId.value = server.id
            repo.setDefaultServer(server.id)

            val plugins = repo.getPlugins(server.id)
            val players = repo.getPlayers(server.id)

            PaperServerService.startService(getApplication())
            engine.startServer(server, plugins, players)
        }
    }

    fun stopServer() {
        engine.stopServer()
    }

    fun restartServer() {
        viewModelScope.launch {
            val server = currentServer.value ?: return@launch
            val plugins = repo.getPlugins(server.id)
            val players = repo.getPlayers(server.id)
            engine.restartServer(server, plugins, players)
        }
    }

    fun sendCommand(cmd: String) {
        if (cmd.isNotBlank()) {
            val list = _commandHistory.value.toMutableList()
            list.remove(cmd)
            list.add(0, cmd)
            _commandHistory.value = list.take(30)
            engine.executeCommand(cmd)
        }
    }

    fun clearConsole() {
        engine.clearLogs()
    }

    fun updateServerConfig(updated: ServerProfileEntity) {
        viewModelScope.launch {
            repo.saveServer(updated)
            if (engine.status.value != ServerStatus.STOPPED && (engine.runningServerId.value == updated.id || currentServer.value?.id == updated.id)) {
                engine.updateProfile(updated)
            }
        }
    }

    fun updateServerMinecraftVersion(newVersion: String) {
        val server = currentServer.value ?: return
        val clean = newVersion.trim()
        if (clean.isBlank()) return
        val updated = server.copy(minecraftVersion = clean)
        viewModelScope.launch {
            repo.saveServer(updated)
            if (engine.status.value != ServerStatus.STOPPED && (engine.runningServerId.value == updated.id || currentServer.value?.id == updated.id)) {
                engine.updateProfile(updated)
            }
        }
    }

    fun updateServerDirectory(newPath: String) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            val finalPath = newPath.trim().ifBlank { null }
            repo.updateServerDirectory(server.id, finalPath)
        }
    }

    fun createServerProfile(
        name: String,
        version: String,
        port: Int,
        motd: String,
        gamemode: String,
        difficulty: String,
        ramMb: Int
    ) {
        viewModelScope.launch {
            val newProfile = ServerProfileEntity(
                name = name,
                minecraftVersion = version,
                paperBuild = 120,
                port = port,
                motd = motd,
                gamemode = gamemode,
                difficulty = difficulty,
                allocatedRamMb = ramMb,
                createdAt = System.currentTimeMillis()
            )
            val newId = repo.saveServer(newProfile)
            if (newId > 0) {
                _selectedServerId.value = newId
                repo.setDefaultServer(newId)
            }
        }
    }

    fun deleteServer(serverId: Long) {
        viewModelScope.launch {
            if (engine.runningServerId.value == serverId) {
                engine.stopServerAwait()
            }
            repo.deleteServerById(serverId)
            val remaining = repo.allServers.first()
            if (remaining.isNotEmpty()) {
                val next = remaining.first()
                _selectedServerId.value = next.id
                repo.setDefaultServer(next.id)
            } else {
                val defaultServer = ServerProfileEntity(
                    name = "Paper Server",
                    minecraftVersion = "1.21.1",
                    isDefault = true
                )
                val newId = repo.saveServer(defaultServer)
                _selectedServerId.value = newId
            }
        }
    }

    fun deleteCurrentServer() {
        val server = currentServer.value ?: return
        deleteServer(server.id)
    }

    fun togglePlugin(plugin: InstalledPluginEntity) {
        viewModelScope.launch {
            repo.setPluginEnabled(plugin.id, !plugin.isEnabled)
        }
    }

    fun installPlugin(
        pluginId: String,
        name: String,
        version: String,
        author: String,
        description: String,
        category: String,
        fileName: String,
        iconUrl: String? = null,
        source: String = "store"
    ) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            val plugin = InstalledPluginEntity(
                serverId = server.id,
                pluginId = pluginId,
                name = name,
                version = version,
                author = author,
                description = description,
                category = category,
                isEnabled = true,
                fileName = fileName,
                fileSizeBytes = (500000L..15000000L).random(),
                iconUrl = iconUrl,
                source = source
            )
            repo.installPlugin(plugin)
        }
    }

    fun deletePlugin(plugin: InstalledPluginEntity) {
        viewModelScope.launch {
            repo.deletePlugin(plugin)
        }
    }

    fun searchModrinth(query: String) {
        _modrinthQuery.value = query
        viewModelScope.launch {
            _isSearchingModrinth.value = true
            val results = repo.searchModrinthPlugins(query)
            _modrinthHits.value = results
            _isSearchingModrinth.value = false
        }
    }

    fun installModrinthPlugin(hit: ModrinthProjectHit, onDone: () -> Unit = {}) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            val category = hit.categories?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Plugin"
            val versionStr = hit.latestVersion ?: "Latest"
            val safeSlug = hit.slug?.ifBlank { hit.title.lowercase().replace(" ", "-") } ?: hit.projectId
            val fileName = "${safeSlug}-$versionStr.jar"
            val plugin = InstalledPluginEntity(
                serverId = server.id,
                pluginId = hit.projectId,
                name = hit.title,
                version = versionStr,
                author = hit.author ?: "Modrinth Creator",
                description = hit.description ?: "Plugin from Modrinth",
                category = category,
                isEnabled = true,
                fileName = fileName,
                fileSizeBytes = (750000L..12000000L).random(),
                source = "modrinth",
                iconUrl = hit.iconUrl
            )
            repo.installPlugin(plugin)
            onDone()
        }
    }

    fun importPluginFile(uri: Uri, onSuccess: (String) -> Unit = {}) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            try {
                val entity = repo.importPluginFile(server.id, uri)
                onSuccess(entity.fileName)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun importServerJarFile(uri: Uri, onSuccess: (String) -> Unit = {}) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            try {
                val updated = repo.importServerJarFile(server.id, uri)
                if (updated != null) {
                    onSuccess(updated.customJarFileName ?: "custom.jar")
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun removeCustomServerJar(onSuccess: () -> Unit = {}) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            repo.removeCustomPaperJar(server.id)
            onSuccess()
        }
    }

    fun savePlayer(player: PlayerRecordEntity) {
        viewModelScope.launch {
            repo.savePlayer(player)
        }
    }

    fun deletePlayer(player: PlayerRecordEntity) {
        viewModelScope.launch {
            repo.deletePlayer(player)
        }
    }

    fun createBackup(title: String) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            repo.createBackup(server.id, title, server.levelName)
        }
    }

    fun deleteBackup(backup: BackupEntity) {
        viewModelScope.launch {
            repo.deleteBackup(backup)
        }
    }

    fun simulatePlayerJoin(name: String) {
        engine.simulatePlayerJoin(name)
    }

    // Pojav Java 1.21.1 Test Client Simulation
    private val _pojavState = MutableStateFlow(PojavClientState())
    val pojavState: StateFlow<PojavClientState> = _pojavState.asStateFlow()

    private val pojavListener = object : PojavSessionListener {
        override fun onServerMessage(message: String) {
            val sender = if (message.startsWith("<") && message.contains(">")) {
                message.substringAfter("<").substringBefore(">")
            } else if (message.startsWith("§d[Server]")) {
                "Server"
            } else {
                "System"
            }
            val chat = PojavChatMessage(
                sender = sender,
                text = message,
                isSystem = !message.startsWith("<")
            )
            _pojavState.value = _pojavState.value.copy(
                chatMessages = (_pojavState.value.chatMessages + chat).takeLast(60)
            )
        }

        override fun onGamemodeChanged(newGamemode: String) {
            _pojavState.value = _pojavState.value.copy(gamemode = newGamemode.uppercase())
        }

        override fun onTeleported(x: Double, y: Double, z: Double) {
            _pojavState.value = _pojavState.value.copy(posX = x, posY = y, posZ = z)
        }
    }

    fun launchPojavClient(connectToServer: Boolean) {
        val current = _pojavState.value
        _pojavState.value = current.copy(
            isRunning = true,
            isConnectedToServer = connectToServer
        )
        if (connectToServer) {
            if (engine.status.value != ServerStatus.RUNNING) {
                startServer()
            }
            engine.registerPojavSession(
                name = current.username,
                initialX = current.posX,
                initialY = current.posY,
                initialZ = current.posZ,
                listener = pojavListener
            )
        }
    }

    fun stopPojavClient() {
        val current = _pojavState.value
        if (current.isConnectedToServer) {
            engine.unregisterPojavSession(current.username)
        }
        _pojavState.value = current.copy(
            isRunning = false,
            isConnectedToServer = false,
            isChatOpen = false,
            isInventoryOpen = false,
            isSettingsOpen = false
        )
    }

    fun togglePojavServerConnection() {
        val current = _pojavState.value
        if (current.isConnectedToServer) {
            engine.unregisterPojavSession(current.username)
            _pojavState.value = current.copy(isConnectedToServer = false)
        } else {
            if (engine.status.value != ServerStatus.RUNNING) {
                startServer()
            }
            engine.registerPojavSession(
                name = current.username,
                initialX = current.posX,
                initialY = current.posY,
                initialZ = current.posZ,
                listener = pojavListener
            )
            _pojavState.value = current.copy(isConnectedToServer = true)
        }
    }

    fun togglePojavF3() {
        _pojavState.value = _pojavState.value.copy(isF3Open = !_pojavState.value.isF3Open)
    }

    fun togglePojavChat() {
        _pojavState.value = _pojavState.value.copy(isChatOpen = !_pojavState.value.isChatOpen)
    }

    fun togglePojavInventory() {
        _pojavState.value = _pojavState.value.copy(isInventoryOpen = !_pojavState.value.isInventoryOpen)
    }

    fun togglePojavSettings() {
        _pojavState.value = _pojavState.value.copy(isSettingsOpen = !_pojavState.value.isSettingsOpen)
    }

    fun selectPojavHotbarSlot(slot: Int) {
        _pojavState.value = _pojavState.value.copy(selectedHotbarSlot = slot.coerceIn(0, 8))
    }

    fun sendPojavChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = _pojavState.value

        val newMsg = PojavChatMessage(
            sender = current.username,
            text = trimmed,
            isSystem = false
        )
        _pojavState.value = current.copy(
            chatMessages = (current.chatMessages + newMsg).takeLast(60)
        )

        if (current.isConnectedToServer) {
            if (trimmed.startsWith("/")) {
                engine.executeCommand(trimmed.substring(1))
            } else {
                engine.broadcastPlayerChat(current.username, trimmed)
            }
        }
    }

    fun updatePojavPosition(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        val current = _pojavState.value
        _pojavState.value = current.copy(
            posX = x,
            posY = y,
            posZ = z,
            yaw = yaw,
            pitch = pitch
        )
        if (current.isConnectedToServer) {
            engine.updatePlayerPosition(current.username, x, y, z, yaw, pitch)
        }
    }

    fun updatePojavSettings(version: String, ramMb: Int, renderer: String, username: String) {
        val oldUsername = _pojavState.value.username
        val wasConnected = _pojavState.value.isConnectedToServer
        if (wasConnected && oldUsername != username) {
            engine.unregisterPojavSession(oldUsername)
        }
        _pojavState.value = _pojavState.value.copy(
            minecraftVersion = version,
            allocatedRamMb = ramMb,
            renderer = renderer,
            username = username
        )
        if (wasConnected && oldUsername != username) {
            engine.registerPojavSession(
                name = username,
                initialX = _pojavState.value.posX,
                initialY = _pojavState.value.posY,
                initialZ = _pojavState.value.posZ,
                listener = pojavListener
            )
        }
    }

    fun getTermuxScript(): String {
        val server = currentServer.value ?: return ""
        return engine.generateTermuxScript(server)
    }

    fun analyzeZipForImport(uri: Uri) {
        viewModelScope.launch {
            _pendingZipUri.value = uri
            _isAnalyzingZip.value = true
            _importErrorMessage.value = null
            _zipAnalysis.value = null
            try {
                val analysis = repo.analyzeServerZip(uri)
                _zipAnalysis.value = analysis
            } catch (e: Exception) {
                _importErrorMessage.value = "Failed to analyze ZIP archive: ${e.message}"
            } finally {
                _isAnalyzingZip.value = false
            }
        }
    }

    fun confirmImportZip(
        options: ImportServerOptions,
        onSuccess: (ServerProfileEntity) -> Unit = {}
    ) {
        val uri = _pendingZipUri.value ?: return
        val analysis = _zipAnalysis.value ?: return

        viewModelScope.launch {
            _isExtractingZip.value = true
            _zipExtractionProgress.value = 0f
            _zipExtractionStatus.value = "Creating server directory and extracting files..."
            _importErrorMessage.value = null

            try {
                val created = repo.importServerFromZip(
                    uri = uri,
                    analysis = analysis,
                    options = options
                ) { progress, status ->
                    _zipExtractionProgress.value = progress
                    _zipExtractionStatus.value = status
                }
                _selectedServerId.value = created.id
                _zipAnalysis.value = null
                _pendingZipUri.value = null
                onSuccess(created)
            } catch (e: Exception) {
                _importErrorMessage.value = "Server import failed: ${e.message}"
            } finally {
                _isExtractingZip.value = false
            }
        }
    }

    fun dismissZipImport() {
        _zipAnalysis.value = null
        _pendingZipUri.value = null
        _isAnalyzingZip.value = false
        _isExtractingZip.value = false
        _importErrorMessage.value = null
    }

    fun loadServerProperties(serverId: Long? = currentServer.value?.id) {
        val id = serverId ?: return
        viewModelScope.launch {
            _isLoadingProperties.value = true
            try {
                val doc = repo.loadServerProperties(id)
                _serverProperties.value = doc
            } catch (e: Exception) {
                // If loading fails, retain or fallback to current profile
                currentServer.value?.let { s ->
                    _serverProperties.value = ServerPropertiesDocument.fromProfile(s)
                }
            } finally {
                _isLoadingProperties.value = false
            }
        }
    }

    // Playit.gg Tunnel State
    val playitTunnelInfo = com.example.server.PlayitTunnelManager.tunnelInfo
    val playitTunnelLogs = com.example.server.PlayitTunnelManager.tunnelLogs

    private val _hostTestResult = MutableStateFlow<com.example.server.HostResolutionResult?>(null)
    val hostTestResult: StateFlow<com.example.server.HostResolutionResult?> = _hostTestResult.asStateFlow()

    private val _isTestingHost = MutableStateFlow(false)
    val isTestingHost: StateFlow<Boolean> = _isTestingHost.asStateFlow()

    fun testHost(address: String) {
        viewModelScope.launch {
            _isTestingHost.value = true
            _hostTestResult.value = com.example.server.PlayitTunnelManager.testHostResolution(address)
            _isTestingHost.value = false
        }
    }

    fun clearHostTestResult() {
        _hostTestResult.value = null
    }

    fun togglePlayitTunnel(enabled: Boolean, customSubdomain: String? = null) {
        val port = currentServer.value?.port ?: 25565
        if (enabled) {
            com.example.server.PlayitTunnelManager.startTunnel(port, customSubdomain)
        } else {
            com.example.server.PlayitTunnelManager.stopTunnel()
        }
    }

    fun updatePlayitPublicAddress(address: String) {
        com.example.server.PlayitTunnelManager.updatePublicAddress(address)
    }

    fun generateNewPlayitClaimLink() {
        com.example.server.PlayitTunnelManager.generateNewClaimLink()
    }

    fun fixPlayitVersionTooOld() {
        com.example.server.PlayitTunnelManager.fixVersionTooOldAndRegenerate()
    }

    fun savePlayitSecretKey(secretKey: String) {
        com.example.server.PlayitTunnelManager.saveSecretKey(secretKey)
    }

    fun installPlayitPlugin(onComplete: (Boolean) -> Unit = {}) {
        val server = currentServer.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val effectiveDir = server.getEffectiveDirectory(getApplication())
            val success = com.example.server.PlayitTunnelManager.installPlayitPlugin(effectiveDir)
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun saveServerProperties(
        properties: ServerPropertiesDocument,
        onDone: () -> Unit = {}
    ) {
        val server = currentServer.value ?: return
        viewModelScope.launch {
            _isSavingProperties.value = true
            try {
                val updatedProfile = repo.saveServerProperties(server.id, properties)
                _serverProperties.value = properties
                if (updatedProfile != null && engine.status.value == ServerStatus.RUNNING) {
                    // Update running engine with new port/motd if applicable
                    engine.updateProfile(updatedProfile)
                }
                onDone()
            } catch (_: Exception) {
            } finally {
                _isSavingProperties.value = false
            }
        }
    }
}
