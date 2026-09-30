package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ServerProfileEntity
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
import com.example.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: MainViewModel,
    onNavigateToProperties: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()
    val availableVersions by viewModel.paperVersions.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Editable state mirrors
    var serverName by remember { mutableStateOf("") }
    var motd by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("25565") }
    var maxPlayers by remember { mutableFloatStateOf(20f) }
    var gamemode by remember { mutableStateOf("survival") }
    var difficulty by remember { mutableStateOf("normal") }
    var pvp by remember { mutableStateOf(true) }
    var onlineMode by remember { mutableStateOf(false) }
    var hardcore by remember { mutableStateOf(false) }
    var allowFlight by remember { mutableStateOf(false) }
    var viewDistance by remember { mutableFloatStateOf(8f) }
    var simulationDistance by remember { mutableFloatStateOf(6f) }
    var ramAllocationMb by remember { mutableFloatStateOf(1024f) }
    var useAikarFlags by remember { mutableStateOf(true) }
    var customJvmArgs by remember { mutableStateOf("") }
    var enableBedrock by remember { mutableStateOf(true) }
    var whitelistEnabled by remember { mutableStateOf(false) }
    var levelName by remember { mutableStateOf("world") }
    var seed by remember { mutableStateOf("") }

    // Paper build selection state
    var minecraftVersion by remember { mutableStateOf("1.21.1") }
    var paperBuild by remember { mutableStateOf("120") }
    var useCustomJar by remember { mutableStateOf(false) }
    var customJarName by remember { mutableStateOf<String?>(null) }
    var customJarSizeBytes by remember { mutableStateOf(0L) }

    var rawPropertiesText by remember { mutableStateOf("") }

    // SAF Document Picker for selecting custom Paper server build .jar
    val serverJarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importServerJarFile(uri) { importedName ->
                customJarName = importedName
                useCustomJar = true
                Toast.makeText(context, "Selected custom server build: $importedName", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Sync state when server changes
    LaunchedEffect(currentServer) {
        currentServer?.let { s ->
            serverName = s.name
            motd = s.motd
            port = s.port.toString()
            maxPlayers = s.maxPlayers.toFloat()
            gamemode = s.gamemode
            difficulty = s.difficulty
            pvp = s.pvp
            onlineMode = s.onlineMode
            hardcore = s.hardcore
            allowFlight = s.allowFlight
            viewDistance = s.viewDistance.toFloat()
            simulationDistance = s.simulationDistance.toFloat()
            ramAllocationMb = s.allocatedRamMb.toFloat()
            useAikarFlags = s.useAikarFlags
            customJvmArgs = s.customJvmArgs
            enableBedrock = s.enableBedrockPing
            whitelistEnabled = s.whiteListEnabled
            levelName = s.levelName
            seed = s.seed

            minecraftVersion = s.minecraftVersion
            paperBuild = s.paperBuild.toString()
            useCustomJar = s.useCustomJar
            customJarName = s.customJarFileName
            customJarSizeBytes = s.customJarSizeBytes

            rawPropertiesText = buildRawServerProperties(s)
        }
    }

    fun saveChanges() {
        val base = currentServer ?: return
        val updated = base.copy(
            name = serverName,
            motd = motd,
            port = port.toIntOrNull() ?: 25565,
            maxPlayers = maxPlayers.toInt(),
            gamemode = gamemode,
            difficulty = difficulty,
            pvp = pvp,
            onlineMode = onlineMode,
            hardcore = hardcore,
            allowFlight = allowFlight,
            viewDistance = viewDistance.toInt(),
            simulationDistance = simulationDistance.toInt(),
            allocatedRamMb = ramAllocationMb.toInt(),
            useAikarFlags = useAikarFlags,
            customJvmArgs = customJvmArgs,
            enableBedrockPing = enableBedrock,
            whiteListEnabled = whitelistEnabled,
            levelName = levelName,
            seed = seed,
            minecraftVersion = minecraftVersion,
            paperBuild = paperBuild.toIntOrNull() ?: 120,
            useCustomJar = useCustomJar,
            customJarFileName = customJarName,
            customJarSizeBytes = customJarSizeBytes
        )
        viewModel.updateServerConfig(updated)
        Toast.makeText(context, "Server configuration saved successfully!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperDarkBg)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PaperSurface,
            contentColor = PaperCyan,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(3.dp)
                        .background(PaperCyan)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Visual Config", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Visual", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_visual_config")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Raw .properties", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Code, contentDescription = "Raw", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_raw_properties")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Termux Runner", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Terminal, contentDescription = "Termux", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_termux_runner")
            )
        }

        when (selectedTab) {
            0 -> {
                // Visual Config Form
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, PaperCyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                .clickable { onNavigateToProperties() }
                                .testTag("open_server_properties_editor_card"),
                            colors = CardDefaults.cardColors(containerColor = PaperCyan.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PaperCyan.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = PaperCyan, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "server.properties Editor",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PaperCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("EDITOR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PaperCyan)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Interactive & raw editor for server-port, motd, difficulty, whitelist, mobs, world & custom properties.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = PaperCyan)
                            }
                        }
                    }

                    if (currentServer?.importedFromZip == true) {
                        item {
                            ConfigSectionCard(title = "ZIP Server Import Details") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PaperPurple.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Dns, contentDescription = null, tint = PaperPurple, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Imported from ZIP Archive", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text("Configuration automatically adapted from start.bat", fontSize = 11.sp, color = PaperGreen)
                                    }
                                }

                                if (!currentServer?.originalStartScript.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    var scriptExpanded by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { scriptExpanded = !scriptExpanded }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Detected start.bat Script Content", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PaperCyan)
                                        Text(if (scriptExpanded) "Hide ▲" else "View Script ▼", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    if (scriptExpanded) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PaperDarkBg)
                                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(6.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = currentServer?.originalStartScript ?: "",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = PaperGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Paper Server Build & Engine Selector
                        ConfigSectionCard(title = "PaperMC Engine & Build Selection") {
                            ConfigSwitchRow(
                                title = "Use Custom Server Build (.jar)",
                                subtitle = "Load your own Paper, Purpur, Folia or custom compiled jar from phone storage",
                                checked = useCustomJar,
                                onCheckedChange = {
                                    useCustomJar = it
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (useCustomJar) {
                                // Custom Jar details
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PaperDarkBg)
                                        .border(1.dp, PaperPurple.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Custom jar",
                                                    tint = PaperPurple,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = customJarName ?: "No custom .jar selected yet",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    ),
                                                    color = Color.White
                                                )
                                            }

                                            if (customJarName != null) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.removeCustomServerJar {
                                                            customJarName = null
                                                            useCustomJar = false
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = PaperRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        if (customJarSizeBytes > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Size: ${FileUtils.formatBytes(customJarSizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        serverJarPickerLauncher.launch(
                                            arrayOf(
                                                "application/java-archive",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("select_custom_paper_build_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PaperPurple,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse Storage", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Server .jar from Phone", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Official PaperMC API build selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text("PaperMC Version", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        var versionExpanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = versionExpanded,
                                            onExpandedChange = { versionExpanded = it }
                                        ) {
                                            OutlinedTextField(
                                                value = "Paper $minecraftVersion",
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionExpanded) },
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                colors = outlinedColors()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = versionExpanded,
                                                onDismissRequest = { versionExpanded = false }
                                            ) {
                                                availableVersions.forEach { ver ->
                                                    DropdownMenuItem(
                                                        text = { Text("Paper $ver") },
                                                        onClick = {
                                                            minecraftVersion = ver
                                                            versionExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = paperBuild,
                                        onValueChange = { paperBuild = it },
                                        label = { Text("Build #") },
                                        modifier = Modifier.weight(0.9f),
                                        colors = outlinedColors()
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ConfigSectionCard(title = "Server Identity & Network") {
                            OutlinedTextField(
                                value = serverName,
                                onValueChange = { serverName = it },
                                label = { Text("Server Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = motd,
                                onValueChange = { motd = it },
                                label = { Text("MOTD (Message of the Day with § colors)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("MOTD Live Preview:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaperDarkBg)
                                    .padding(10.dp)
                            ) {
                                MinecraftText(text = motd)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = port,
                                    onValueChange = { port = it },
                                    label = { Text("Server Port") },
                                    modifier = Modifier.weight(1f),
                                    colors = outlinedColors()
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Max Players: ${maxPlayers.toInt()}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Slider(
                                        value = maxPlayers,
                                        onValueChange = { maxPlayers = it },
                                        valueRange = 1f..100f,
                                        steps = 98,
                                        colors = SliderDefaults.colors(thumbColor = PaperCyan, activeTrackColor = PaperCyan)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ConfigSectionCard(title = "Gameplay & World Mechanics") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Gamemode selector
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Gamemode", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = gamemode.replaceFirstChar { it.uppercase() },
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                            modifier = Modifier.menuAnchor(),
                                            colors = outlinedColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            listOf("survival", "creative", "adventure", "spectator").forEach { gm ->
                                                DropdownMenuItem(
                                                    text = { Text(gm.replaceFirstChar { it.uppercase() }) },
                                                    onClick = {
                                                        gamemode = gm
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Difficulty selector
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Difficulty", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    var diffExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = diffExpanded,
                                        onExpandedChange = { diffExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = difficulty.replaceFirstChar { it.uppercase() },
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffExpanded) },
                                            modifier = Modifier.menuAnchor(),
                                            colors = outlinedColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = diffExpanded,
                                            onDismissRequest = { diffExpanded = false }
                                        ) {
                                            listOf("peaceful", "easy", "normal", "hard").forEach { diff ->
                                                DropdownMenuItem(
                                                    text = { Text(diff.replaceFirstChar { it.uppercase() }) },
                                                    onClick = {
                                                        difficulty = diff
                                                        diffExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ConfigSwitchRow(
                                title = "PvP (Player vs Player)",
                                subtitle = "Allow players to combat each other",
                                checked = pvp,
                                onCheckedChange = { pvp = it }
                            )

                            ConfigSwitchRow(
                                title = "Hardcore Mode",
                                subtitle = "World locks into hard and permadeath on death",
                                checked = hardcore,
                                onCheckedChange = { hardcore = it }
                            )

                            ConfigSwitchRow(
                                title = "Allow Flight",
                                subtitle = "Allow survival flight plugins or abilities",
                                checked = allowFlight,
                                onCheckedChange = { allowFlight = it }
                            )

                            ConfigSwitchRow(
                                title = "Online Mode (Mojang Authentication)",
                                subtitle = "Turn OFF for offline / cracked accounts & easiest LAN play",
                                checked = onlineMode,
                                onCheckedChange = { onlineMode = it }
                            )

                            ConfigSwitchRow(
                                title = "Bedrock LAN Discovery Ping",
                                subtitle = "Makes server show under Minecraft Bedrock 'Friends' tab",
                                checked = enableBedrock,
                                onCheckedChange = { enableBedrock = it }
                            )

                            ConfigSwitchRow(
                                title = "Enforce Whitelist",
                                subtitle = "Only players listed in Whitelist can join",
                                checked = whitelistEnabled,
                                onCheckedChange = { whitelistEnabled = it }
                            )
                        }
                    }

                    item {
                        ConfigSectionCard(title = "Performance & View Distance") {
                            Text(
                                "View Distance: ${viewDistance.toInt()} chunks",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Slider(
                                value = viewDistance,
                                onValueChange = { viewDistance = it },
                                valueRange = 4f..20f,
                                steps = 15,
                                colors = SliderDefaults.colors(thumbColor = PaperGreen, activeTrackColor = PaperGreen)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "Simulation Distance: ${simulationDistance.toInt()} chunks",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Slider(
                                value = simulationDistance,
                                onValueChange = { simulationDistance = it },
                                valueRange = 3f..12f,
                                steps = 8,
                                colors = SliderDefaults.colors(thumbColor = PaperCyan, activeTrackColor = PaperCyan)
                            )
                        }
                    }

                    item {
                        ConfigSectionCard(title = "JVM Memory & Tuning") {
                            Text(
                                "Allocated Memory (RAM): ${ramAllocationMb.toInt()} MB",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PaperCyan
                            )
                            Slider(
                                value = ramAllocationMb,
                                onValueChange = { ramAllocationMb = it },
                                valueRange = 512f..4096f,
                                steps = 13,
                                colors = SliderDefaults.colors(thumbColor = PaperCyan, activeTrackColor = PaperCyan)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ConfigSwitchRow(
                                title = "PaperMC Aikar's G1GC Flags",
                                subtitle = "Optimized Garbage Collector flags recommended by PaperMC team",
                                checked = useAikarFlags,
                                onCheckedChange = { useAikarFlags = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customJvmArgs,
                                onValueChange = { customJvmArgs = it },
                                label = { Text("Custom JVM Arguments") },
                                placeholder = { Text("-Dpaper.playerconnection.keepalive=30") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { saveChanges() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_config_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save All Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            1 -> {
                // Raw properties tab
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
                            "server.properties",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = PaperCyan
                        )
                        FilledTonalButton(
                            onClick = onNavigateToProperties,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PaperCyan.copy(alpha = 0.2f), contentColor = PaperCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("tab1_open_full_editor_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Dedicated Editor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawPropertiesText,
                        onValueChange = { rawPropertiesText = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
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
                        onClick = {
                            parseAndApplyRawProperties(rawPropertiesText) { updated ->
                                viewModel.updateServerConfig(updated)
                                Toast.makeText(context, "Saved server.properties successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Raw")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply server.properties", fontWeight = FontWeight.Bold)
                    }
                }
            }

            2 -> {
                // Termux Native Runner Helper
                val termuxScript = remember(currentServer) {
                    viewModel.getTermuxScript()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = PaperSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Run via Termux (Native OpenJDK 21)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PaperCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "For power users who want to run the full official PaperMC .jar with Java 21 directly on this Android device, PaperCraft generates the complete self-installing launch script.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = PaperConsoleBg),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("launcher.sh", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Termux Script", termuxScript))
                                            Toast.makeText(context, "Termux script copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Script", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = termuxScript,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFFB0BEC5)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigSectionCard(
    title: String,
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PaperCyan
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun ConfigSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PaperDarkBg,
                checkedTrackColor = PaperGreen,
                uncheckedTrackColor = PaperSurfaceElevated
            )
        )
    }
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PaperConsoleBg,
    unfocusedContainerColor = PaperConsoleBg,
    focusedBorderColor = PaperCyan,
    unfocusedBorderColor = PaperSurfaceBorder,
    focusedLabelColor = PaperCyan,
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

fun buildRawServerProperties(s: ServerProfileEntity): String {
    return """
        # Minecraft server.properties generated by PaperCraft
        # ${java.util.Date()}
        server-name=${s.name}
        motd=${s.motd}
        server-port=${s.port}
        max-players=${s.maxPlayers}
        gamemode=${s.gamemode}
        difficulty=${s.difficulty}
        pvp=${s.pvp}
        online-mode=${s.onlineMode}
        hardcore=${s.hardcore}
        allow-flight=${s.allowFlight}
        view-distance=${s.viewDistance}
        simulation-distance=${s.simulationDistance}
        spawn-protection=${s.spawnProtection}
        white-list=${s.whiteListEnabled}
        level-name=${s.levelName}
        level-seed=${s.seed}
        enable-status=true
        rate-limit=0
        network-compression-threshold=256
    """.trimIndent()
}

fun parseAndApplyRawProperties(
    raw: String,
    onUpdated: (ServerProfileEntity) -> Unit
) {
    var updated = ServerProfileEntity(name = "Paper Server")
    raw.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("#") || !trimmed.contains("=")) return@forEach
        val parts = trimmed.split("=", limit = 2)
        val k = parts[0].trim()
        val v = parts[1].trim()
        when (k) {
            "server-name" -> updated = updated.copy(name = v)
            "motd" -> updated = updated.copy(motd = v)
            "server-port" -> v.toIntOrNull()?.let { updated = updated.copy(port = it) }
            "max-players" -> v.toIntOrNull()?.let { updated = updated.copy(maxPlayers = it) }
            "gamemode" -> updated = updated.copy(gamemode = v)
            "difficulty" -> updated = updated.copy(difficulty = v)
            "pvp" -> updated = updated.copy(pvp = v.toBoolean())
            "online-mode" -> updated = updated.copy(onlineMode = v.toBoolean())
            "hardcore" -> updated = updated.copy(hardcore = v.toBoolean())
            "allow-flight" -> updated = updated.copy(allowFlight = v.toBoolean())
            "view-distance" -> v.toIntOrNull()?.let { updated = updated.copy(viewDistance = it) }
            "simulation-distance" -> v.toIntOrNull()?.let { updated = updated.copy(simulationDistance = it) }
            "level-name" -> updated = updated.copy(levelName = v)
            "level-seed" -> updated = updated.copy(seed = v)
            "white-list" -> updated = updated.copy(whiteListEnabled = v.toBoolean())
        }
    }
    onUpdated(updated)
}
