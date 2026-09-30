package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.MinecraftText
import com.example.ui.theme.PaperConsoleBg
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGold
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperPurple
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import com.example.ui.theme.PaperSurfaceElevated
import com.example.utils.ServerPropertiesDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerPropertiesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()
    val serverProperties by viewModel.serverProperties.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingProperties.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingProperties.collectAsStateWithLifecycle()

    BackHandler {
        onNavigateBack()
    }

    var selectedModeTab by remember { mutableIntStateOf(0) } // 0 = Visual Editor, 1 = Raw File
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    // Editable state mirrors
    var serverPort by remember { mutableStateOf("25565") }
    var motd by remember { mutableStateOf("§aA Minecraft Server") }
    var difficulty by remember { mutableStateOf("normal") }
    var gamemode by remember { mutableStateOf("survival") }
    var maxPlayers by remember { mutableFloatStateOf(20f) }
    var pvp by remember { mutableStateOf(true) }
    var onlineMode by remember { mutableStateOf(false) }
    var hardcore by remember { mutableStateOf(false) }
    var allowFlight by remember { mutableStateOf(false) }
    var whiteList by remember { mutableStateOf(false) }
    var enforceWhitelist by remember { mutableStateOf(false) }
    var viewDistance by remember { mutableFloatStateOf(10f) }
    var simulationDistance by remember { mutableFloatStateOf(8f) }
    var spawnProtection by remember { mutableFloatStateOf(16f) }
    var levelName by remember { mutableStateOf("world") }
    var levelSeed by remember { mutableStateOf("") }
    var levelType by remember { mutableStateOf("minecraft:normal") }
    var spawnMonsters by remember { mutableStateOf(true) }
    var spawnAnimals by remember { mutableStateOf(true) }
    var spawnNpcs by remember { mutableStateOf(true) }
    var generateStructures by remember { mutableStateOf(true) }
    var allowNether by remember { mutableStateOf(true) }
    var enableCommandBlock by remember { mutableStateOf(false) }
    var syncChunkWrites by remember { mutableStateOf(true) }
    var networkCompressionThreshold by remember { mutableStateOf("256") }
    var maxTickTime by remember { mutableStateOf("60000") }
    var enableStatus by remember { mutableStateOf(true) }
    var resourcePack by remember { mutableStateOf("") }

    val customPropsMap = remember { mutableStateMapOf<String, String>() }
    var rawTextState by remember { mutableStateOf("") }

    // Sync from ViewModel state
    fun syncFromDocument(doc: ServerPropertiesDocument) {
        serverPort = doc.serverPort.toString()
        motd = doc.motd
        difficulty = doc.difficulty
        gamemode = doc.gamemode
        maxPlayers = doc.maxPlayers.toFloat()
        pvp = doc.pvp
        onlineMode = doc.onlineMode
        hardcore = doc.hardcore
        allowFlight = doc.allowFlight
        whiteList = doc.whiteList
        enforceWhitelist = doc.enforceWhitelist
        viewDistance = doc.viewDistance.toFloat()
        simulationDistance = doc.simulationDistance.toFloat()
        spawnProtection = doc.spawnProtection.toFloat()
        levelName = doc.levelName
        levelSeed = doc.levelSeed
        levelType = doc.levelType
        spawnMonsters = doc.spawnMonsters
        spawnAnimals = doc.spawnAnimals
        spawnNpcs = doc.spawnNpcs
        generateStructures = doc.generateStructures
        allowNether = doc.allowNether
        enableCommandBlock = doc.enableCommandBlock
        syncChunkWrites = doc.syncChunkWrites
        networkCompressionThreshold = doc.networkCompressionThreshold.toString()
        maxTickTime = doc.maxTickTime.toString()
        enableStatus = doc.enableStatus
        resourcePack = doc.resourcePack

        customPropsMap.clear()
        customPropsMap.putAll(doc.customProperties)
        rawTextState = doc.toPropertiesFileText()
    }

    LaunchedEffect(serverProperties) {
        serverProperties?.let { syncFromDocument(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadServerProperties()
    }

    fun buildCurrentDocument(): ServerPropertiesDocument {
        return ServerPropertiesDocument(
            serverPort = serverPort.toIntOrNull() ?: 25565,
            motd = motd,
            difficulty = difficulty,
            gamemode = gamemode,
            maxPlayers = maxPlayers.toInt(),
            pvp = pvp,
            onlineMode = onlineMode,
            hardcore = hardcore,
            allowFlight = allowFlight,
            whiteList = whiteList,
            enforceWhitelist = enforceWhitelist,
            viewDistance = viewDistance.toInt(),
            simulationDistance = simulationDistance.toInt(),
            spawnProtection = spawnProtection.toInt(),
            levelName = levelName,
            levelSeed = levelSeed,
            levelType = levelType,
            spawnMonsters = spawnMonsters,
            spawnAnimals = spawnAnimals,
            spawnNpcs = spawnNpcs,
            generateStructures = generateStructures,
            allowNether = allowNether,
            enableCommandBlock = enableCommandBlock,
            syncChunkWrites = syncChunkWrites,
            networkCompressionThreshold = networkCompressionThreshold.toIntOrNull() ?: 256,
            maxTickTime = maxTickTime.toLongOrNull() ?: 60000L,
            enableStatus = enableStatus,
            resourcePack = resourcePack,
            customProperties = customPropsMap.toMap()
        )
    }

    fun saveAllProperties() {
        val docToSave = if (selectedModeTab == 1) {
            // Raw mode: parse rawTextState into document
            ServerPropertiesDocument.fromText(rawTextState)
        } else {
            buildCurrentDocument()
        }

        viewModel.saveServerProperties(docToSave) {
            Toast.makeText(context, "server.properties saved & applied successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PaperDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "server.properties",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PaperCyan.copy(alpha = 0.2f))
                                    .border(1.dp, PaperCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EDITOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PaperCyan)
                            }
                        }
                        Text(
                            text = currentServer?.name ?: "Active Profile",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("server_properties_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search properties",
                            tint = if (isSearchActive) PaperCyan else Color.LightGray
                        )
                    }

                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_properties_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Defaults",
                            tint = Color.LightGray
                        )
                    }

                    Button(
                        onClick = { saveAllProperties() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(38.dp)
                            .testTag("save_server_properties_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaperGreen,
                            contentColor = PaperDarkBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PaperDarkBg, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Tabs (Interactive vs Raw File)
            TabRow(
                selectedTabIndex = selectedModeTab,
                containerColor = PaperSurface,
                contentColor = PaperCyan,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedModeTab])
                            .height(3.dp)
                            .background(PaperCyan)
                    )
                }
            ) {
                Tab(
                    selected = selectedModeTab == 0,
                    onClick = {
                        if (selectedModeTab == 1) {
                            // Sync raw text to fields when switching back to visual
                            val parsed = ServerPropertiesDocument.fromText(rawTextState)
                            syncFromDocument(parsed)
                        }
                        selectedModeTab = 0
                    },
                    text = { Text("Visual Editor", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Visual", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_properties_visual")
                )
                Tab(
                    selected = selectedModeTab == 1,
                    onClick = {
                        if (selectedModeTab == 0) {
                            // Update raw text with current visual form before showing raw
                            rawTextState = buildCurrentDocument().toPropertiesFileText()
                        }
                        selectedModeTab = 1
                    },
                    text = { Text("Raw .properties", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Raw", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_properties_raw")
                )
            }

            // Quick Search Bar when active
            AnimatedVisibility(visible = isSearchActive && selectedModeTab == 0) {
                Surface(color = PaperSurfaceElevated) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("properties_search_input"),
                        placeholder = { Text("Filter properties (e.g. port, motd, diff, pvp)...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PaperCyan) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = outlinedColors()
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaperCyan)
                }
            } else if (selectedModeTab == 0) {
                // VISUAL EDITOR
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 1. LIVE MINECRAFT SERVER LIST PING PREVIEW
                    item {
                        MinecraftServerListPreviewCard(
                            serverName = currentServer?.name ?: "Paper Server",
                            motd = motd,
                            port = serverPort.toIntOrNull() ?: 25565,
                            maxPlayers = maxPlayers.toInt(),
                            onlinePlayers = 0,
                            version = currentServer?.minecraftVersion ?: "1.21.1"
                        )
                    }

                    // 2. COMMON & ESSENTIAL SETTINGS SECTION
                    if (searchMatches("port server-port motd difficulty gamemode max-players pvp online-mode", searchQuery)) {
                        item {
                            PropertiesSectionCard(
                                title = "Common Values",
                                subtitle = "Key settings that define how players connect and play",
                                icon = Icons.Default.Tune,
                                tint = PaperCyan
                            ) {
                                // SERVER-PORT
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("server-port", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                            Text("Listening TCP port for Minecraft connections", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilterChip(
                                                selected = serverPort == "25565",
                                                onClick = { serverPort = "25565" },
                                                label = { Text("25565", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaperCyan.copy(alpha = 0.25f), selectedLabelColor = PaperCyan)
                                            )
                                            FilterChip(
                                                selected = serverPort == "19132",
                                                onClick = { serverPort = "19132" },
                                                label = { Text("19132", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaperPurple.copy(alpha = 0.25f), selectedLabelColor = PaperPurple)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = serverPort,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() } && input.length <= 5) {
                                                serverPort = input
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("input_server_port"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = outlinedColors()
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // MOTD (MESSAGE OF THE DAY) WITH COLOR PALETTE
                                Column {
                                    Text("motd (Message of the Day)", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Server list banner text. Tap a color code to insert formatting.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Color codes toolbar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val codes = listOf(
                                            "§a" to Color(0xFF55FF55),
                                            "§b" to Color(0xFF55FFFF),
                                            "§c" to Color(0xFFFF5555),
                                            "§d" to Color(0xFFFF55FF),
                                            "§e" to Color(0xFFFFFF55),
                                            "§f" to Color(0xFFFFFFFF),
                                            "§6" to Color(0xFFFFAA00),
                                            "§5" to Color(0xFFAA00AA),
                                            "§9" to Color(0xFF5555FF),
                                            "§l" to Color.LightGray,
                                            "§r" to Color.Gray
                                        )

                                        codes.forEach { (code, c) ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PaperSurfaceElevated)
                                                    .border(1.dp, c.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                    .clickable { motd += code }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = code,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = c
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = motd,
                                        onValueChange = { motd = it },
                                        modifier = Modifier.fillMaxWidth().testTag("input_server_motd"),
                                        colors = outlinedColors()
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // DIFFICULTY SELECTOR
                                Column {
                                    Text("difficulty", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Affects mob health, player damage, hunger, and world challenges", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val difficulties = listOf(
                                        Triple("peaceful", "Peaceful", PaperGreen),
                                        Triple("easy", "Easy", PaperCyan),
                                        Triple("normal", "Normal", PaperGold),
                                        Triple("hard", "Hard", PaperRed)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        difficulties.forEach { (diffKey, diffLabel, diffColor) ->
                                            val isSelected = difficulty == diffKey
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) diffColor else PaperSurfaceBorder,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { difficulty = diffKey }
                                                    .testTag("difficulty_$diffKey"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) diffColor.copy(alpha = 0.2f) else PaperSurfaceElevated
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = diffLabel,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) diffColor else Color.LightGray,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // GAMEMODE SELECTOR
                                Column {
                                    Text("gamemode", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("Default game mode for new incoming players", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val gamemodes = listOf(
                                        "survival" to "Survival",
                                        "creative" to "Creative",
                                        "adventure" to "Adventure",
                                        "spectator" to "Spectator"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        gamemodes.forEach { (gmKey, gmLabel) ->
                                            val isSelected = gamemode == gmKey
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { gamemode = gmKey },
                                                label = { Text(gmLabel, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PaperCyan.copy(alpha = 0.25f),
                                                    selectedLabelColor = PaperCyan
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    borderColor = PaperSurfaceBorder,
                                                    selectedBorderColor = PaperCyan
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // MAX PLAYERS
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("max-players", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = "${maxPlayers.toInt()} players",
                                            fontWeight = FontWeight.Bold,
                                            color = PaperGreen,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Slider(
                                        value = maxPlayers,
                                        onValueChange = { maxPlayers = it },
                                        valueRange = 1f..100f,
                                        steps = 98,
                                        colors = SliderDefaults.colors(thumbColor = PaperGreen, activeTrackColor = PaperGreen)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(5f, 10f, 20f, 40f, 80f).forEach { cap ->
                                            FilterChip(
                                                selected = maxPlayers == cap,
                                                onClick = { maxPlayers = cap },
                                                label = { Text("${cap.toInt()}", fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaperGreen.copy(alpha = 0.25f), selectedLabelColor = PaperGreen)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. SECURITY & ACCESS
                    if (searchMatches("security online-mode pvp hardcore white-list whitelist allow-flight", searchQuery)) {
                        item {
                            PropertiesSectionCard(
                                title = "Security & Access",
                                subtitle = "Authentication, player combat, permissions, and whitelist",
                                icon = Icons.Default.Security,
                                tint = PaperPurple
                            ) {
                                PropertySwitchRow(
                                    key = "online-mode",
                                    label = "Online Mode (Mojang Authentication)",
                                    description = if (onlineMode) "Official Mojang session check enabled (Java accounts only)" else "Offline / Cracked mode: Allows Bedrock (Geyser) & unauthenticated players",
                                    checked = onlineMode,
                                    onCheckedChange = { onlineMode = it },
                                    accentColor = PaperPurple
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "pvp",
                                    label = "Player vs Player Combat",
                                    description = "Enable swords, arrows, and attacks between players",
                                    checked = pvp,
                                    onCheckedChange = { pvp = it },
                                    accentColor = PaperRed
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "hardcore",
                                    label = "Hardcore Mode",
                                    description = "Permanent death! Players are locked into Spectator mode upon dying",
                                    checked = hardcore,
                                    onCheckedChange = { hardcore = it },
                                    accentColor = PaperRed
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "allow-flight",
                                    label = "Allow Flight",
                                    description = "Allows players in Survival mode to fly (prevents 'Flying is not enabled' kicks)",
                                    checked = allowFlight,
                                    onCheckedChange = { allowFlight = it },
                                    accentColor = PaperCyan
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "white-list",
                                    label = "Enforce Whitelist",
                                    description = "Only players listed in whitelist.json can connect",
                                    checked = whiteList,
                                    onCheckedChange = {
                                        whiteList = it
                                        if (it) enforceWhitelist = true
                                    },
                                    accentColor = PaperGold
                                )
                            }
                        }
                    }

                    // 4. WORLD & GENERATION
                    if (searchMatches("world level-name level-seed nether structures monsters animals spawn-protection", searchQuery)) {
                        item {
                            PropertiesSectionCard(
                                title = "World & Generation",
                                subtitle = "Dimensions, mob spawning, world seed, and protected zones",
                                icon = Icons.Default.Public,
                                tint = PaperGreen
                            ) {
                                OutlinedTextField(
                                    value = levelName,
                                    onValueChange = { levelName = it },
                                    label = { Text("level-name (World folder)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = outlinedColors()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = levelSeed,
                                    onValueChange = { levelSeed = it },
                                    label = { Text("level-seed (Generation seed)") },
                                    placeholder = { Text("Leave blank for random seed") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = outlinedColors()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("spawn-protection", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text("${spawnProtection.toInt()} blocks radius", fontWeight = FontWeight.Bold, color = PaperCyan, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = spawnProtection,
                                        onValueChange = { spawnProtection = it },
                                        valueRange = 0f..64f,
                                        steps = 63,
                                        colors = SliderDefaults.colors(thumbColor = PaperCyan, activeTrackColor = PaperCyan)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "spawn-monsters",
                                    label = "Spawn Monsters",
                                    description = "Zombies, Skeletons, Creepers, and hostile entities",
                                    checked = spawnMonsters,
                                    onCheckedChange = { spawnMonsters = it },
                                    accentColor = PaperRed
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "spawn-animals",
                                    label = "Spawn Passive Animals",
                                    description = "Cows, Pigs, Sheep, Chickens, and horses",
                                    checked = spawnAnimals,
                                    onCheckedChange = { spawnAnimals = it },
                                    accentColor = PaperGreen
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "allow-nether",
                                    label = "Allow Nether Dimension",
                                    description = "Enables portals and access to The Nether dimension",
                                    checked = allowNether,
                                    onCheckedChange = { allowNether = it },
                                    accentColor = PaperGold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "enable-command-block",
                                    label = "Enable Command Blocks",
                                    description = "Permits server command block execution",
                                    checked = enableCommandBlock,
                                    onCheckedChange = { enableCommandBlock = it },
                                    accentColor = PaperPurple
                                )
                            }
                        }
                    }

                    // 5. PERFORMANCE & CHUNKS
                    if (searchMatches("performance view-distance simulation-distance sync compression tick", searchQuery)) {
                        item {
                            PropertiesSectionCard(
                                title = "Performance & Limits",
                                subtitle = "Chunk distances, packet compression, and tick budget",
                                icon = Icons.Default.Speed,
                                tint = PaperGold
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("view-distance", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text("${viewDistance.toInt()} chunks", fontWeight = FontWeight.Bold, color = PaperGreen, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = viewDistance,
                                        onValueChange = { viewDistance = it },
                                        valueRange = 4f..20f,
                                        steps = 15,
                                        colors = SliderDefaults.colors(thumbColor = PaperGreen, activeTrackColor = PaperGreen)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("simulation-distance", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text("${simulationDistance.toInt()} chunks", fontWeight = FontWeight.Bold, color = PaperCyan, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = simulationDistance,
                                        onValueChange = { simulationDistance = it },
                                        valueRange = 3f..16f,
                                        steps = 12,
                                        colors = SliderDefaults.colors(thumbColor = PaperCyan, activeTrackColor = PaperCyan)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                PropertySwitchRow(
                                    key = "sync-chunk-writes",
                                    label = "Sync Chunk Writes",
                                    description = "Synchronously flush chunk changes to internal flash storage",
                                    checked = syncChunkWrites,
                                    onCheckedChange = { syncChunkWrites = it },
                                    accentColor = PaperGold
                                )
                            }
                        }
                    }

                    // 6. CUSTOM & EXTRA PROPERTIES
                    item {
                        PropertiesSectionCard(
                            title = "Custom & Third-Party Properties",
                            subtitle = "Additional settings preserved from server.properties",
                            icon = Icons.Default.Code,
                            tint = PaperPurple
                        ) {
                            if (customPropsMap.isEmpty()) {
                                Text(
                                    text = "No custom properties present. Tap 'Add Custom Property' to add any specialized plugin or server key.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            } else {
                                customPropsMap.forEach { (key, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(key, fontWeight = FontWeight.SemiBold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                            Text(value, color = PaperCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = { customPropsMap.remove(key) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PaperRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showAddCustomDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PaperSurfaceElevated, contentColor = PaperPurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("add_custom_prop_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Property", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Bottom Save Button
                    item {
                        Button(
                            onClick = { saveAllProperties() },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("bottom_save_properties_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PaperDarkBg, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save & Apply server.properties", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            } else {
                // RAW FILE EDITOR TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Direct File Contents",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PaperCyan
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    // Format / Prettify raw text
                                    val doc = ServerPropertiesDocument.fromText(rawTextState)
                                    rawTextState = doc.toPropertiesFileText()
                                    Toast.makeText(context, "Formatted server.properties", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.FormatPaint, contentDescription = "Format", tint = PaperCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Prettify", color = PaperCyan, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawTextState,
                        onValueChange = { rawTextState = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("raw_properties_textarea"),
                        colors = outlinedColors(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFECEFF1)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { saveAllProperties() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("raw_save_properties_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PaperDarkBg, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save Raw")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply server.properties", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Reset Defaults Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = PaperSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = PaperGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Defaults?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "This will restore standard PaperMC server.properties defaults (Port 25565, Survival, Normal difficulty, 20 players, 10 view distance).",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val defaultDoc = ServerPropertiesDocument()
                        syncFromDocument(defaultDoc)
                        showResetDialog = false
                        Toast.makeText(context, "Defaults loaded. Tap Save to apply.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGold, contentColor = PaperDarkBg)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Add Custom Property Dialog
    if (showAddCustomDialog) {
        var newKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            containerColor = PaperSurface,
            title = { Text("Add Custom Property", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it.trim() },
                        label = { Text("Property Key (e.g. query.port)") },
                        singleLine = true,
                        colors = outlinedColors()
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value") },
                        singleLine = true,
                        colors = outlinedColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            customPropsMap[newKey] = newValue
                            showAddCustomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperPurple, contentColor = Color.White)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun MinecraftServerListPreviewCard(
    serverName: String,
    motd: String,
    port: Int,
    maxPlayers: Int,
    onlinePlayers: Int,
    version: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperConsoleBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MINECRAFT MULTIPLAYER PREVIEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaperCyan,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SignalCellularAlt,
                        contentDescription = "Ping",
                        tint = PaperGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Port $port • Paper $version",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                // Server Icon Placeholder
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaperSurfaceElevated)
                        .border(1.dp, PaperCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        tint = PaperCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serverName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "$onlinePlayers/$maxPlayers",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.LightGray
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Formatted MOTD render
                    MinecraftText(
                        text = motd,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        defaultColor = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}

@Composable
fun PropertiesSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun PropertySwitchRow(
    key: String,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PaperSurfaceElevated.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = key,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (checked) accentColor else Color.LightGray
            )
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(description, fontSize = 10.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PaperDarkBg,
                checkedTrackColor = accentColor,
                uncheckedTrackColor = PaperSurfaceBorder
            )
        )
    }
}

private fun searchMatches(text: String, query: String): Boolean {
    if (query.isBlank()) return true
    return text.contains(query, ignoreCase = true)
}
