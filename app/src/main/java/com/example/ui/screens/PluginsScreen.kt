package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.InstalledPluginEntity
import com.example.data.remote.ModrinthProjectHit
import com.example.ui.MainViewModel
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
import java.util.Locale

data class CatalogPlugin(
    val pluginId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: String,
    val fileName: String
)

@Composable
fun PluginsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedPlugins by viewModel.currentPlugins.collectAsStateWithLifecycle()
    val modrinthHits by viewModel.modrinthHits.collectAsStateWithLifecycle()
    val isSearchingModrinth by viewModel.isSearchingModrinth.collectAsStateWithLifecycle()
    val modrinthQuery by viewModel.modrinthQuery.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCustomPluginDialog by remember { mutableStateOf(false) }
    var searchInputText by remember { mutableStateOf(modrinthQuery) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // SAF Document Picker launcher for selecting custom .jar files directly from phone storage
    val pluginFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPluginFile(uri) { importedFileName ->
                Toast.makeText(context, "Imported plugin: $importedFileName", Toast.LENGTH_LONG).show()
            }
        }
    }

    val curatedCatalog = listOf(
        CatalogPlugin(
            pluginId = "geyser",
            name = "GeyserMC",
            version = "2.4.2-SNAPSHOT",
            author = "GeyserMC Team",
            description = "Enables Minecraft Bedrock Edition (iOS, Android, Switch, Xbox, PS) cross-play into this Java server.",
            category = "Crossplay",
            fileName = "Geyser-Spigot.jar"
        ),
        CatalogPlugin(
            pluginId = "floodgate",
            name = "Floodgate",
            version = "2.2.3-SNAPSHOT",
            author = "GeyserMC Team",
            description = "Allows Bedrock players to join without needing a Java Edition account.",
            category = "Authentication",
            fileName = "floodgate-spigot.jar"
        ),
        CatalogPlugin(
            pluginId = "essentialsx",
            name = "EssentialsX",
            version = "2.20.1",
            author = "EssentialsX Team",
            description = "Core suite of 100+ commands: /spawn, /home, /sethome, /tpa, /warp, kits, and economy.",
            category = "Utility",
            fileName = "EssentialsX-2.20.1.jar"
        ),
        CatalogPlugin(
            pluginId = "luckperms",
            name = "LuckPerms",
            version = "5.4.102",
            author = "Luck",
            description = "Industry standard permissions manager with web editor and fast group hierarchies.",
            category = "Administration",
            fileName = "LuckPerms-Bukkit-5.4.102.jar"
        ),
        CatalogPlugin(
            pluginId = "viaversion",
            name = "ViaVersion",
            version = "5.0.3",
            author = "ViaVersion Team",
            description = "Allows newer client versions to join your Paper server.",
            category = "Compatibility",
            fileName = "ViaVersion-5.0.3.jar"
        ),
        CatalogPlugin(
            pluginId = "viabackwards",
            name = "ViaBackwards",
            version = "5.0.3",
            author = "ViaVersion Team",
            description = "Allows older client versions to connect to your newer server.",
            category = "Compatibility",
            fileName = "ViaBackwards-5.0.3.jar"
        ),
        CatalogPlugin(
            pluginId = "clearlag",
            name = "ClearLag",
            version = "3.2.2",
            author = "bob7",
            description = "Reduces server lag by clearing dropped items and throttling mob entity spawns on mobile.",
            category = "Performance",
            fileName = "ClearLag.jar"
        ),
        CatalogPlugin(
            pluginId = "worldedit",
            name = "WorldEdit",
            version = "7.3.6",
            author = "EngineHub",
            description = "In-game map editing, brush tools, block selection, copying and schematics.",
            category = "Creation",
            fileName = "worldedit-bukkit-7.3.6.jar"
        ),
        CatalogPlugin(
            pluginId = "chunky",
            name = "Chunky",
            version = "1.4.16",
            author = "pop4959",
            description = "Pre-generates chunks efficiently to eliminate exploration lag and stutter on phones.",
            category = "Performance",
            fileName = "Chunky-1.4.16.jar"
        ),
        CatalogPlugin(
            pluginId = "vault",
            name = "Vault",
            version = "1.7.3",
            author = "MilkBowl",
            description = "Permissions and economy abstraction API used by most plugins.",
            category = "API",
            fileName = "Vault.jar"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperDarkBg)
    ) {
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
                text = { Text("Installed (${installedPlugins.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Extension, contentDescription = "Installed", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_installed_plugins")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Modrinth Hub", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Modrinth", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_modrinth_plugins")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Curated", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Curated", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_curated_plugins")
            )
        }

        when (selectedTab) {
            0 -> {
                // Installed Plugins List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = PaperSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Custom File & Storage Loader",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PaperCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Pick any Minecraft plugin .jar from your phone storage (Downloads, SD card, files), or install directly from Modrinth.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            pluginFilePickerLauncher.launch(
                                                arrayOf(
                                                    "application/java-archive",
                                                    "application/octet-stream",
                                                    "*/*"
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_custom_plugin_file_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PaperGreen,
                                            contentColor = PaperDarkBg
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = "Pick .jar", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select .jar File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { showCustomPluginDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Manual name", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Manual Entry", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Installed Plugins (${installedPlugins.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    if (installedPlugins.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Extension, contentDescription = "No plugins", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No plugins installed yet.", color = Color.Gray)
                                    Text("Browse Modrinth or select a .jar from storage.", fontSize = 12.sp, color = PaperCyan)
                                }
                            }
                        }
                    } else {
                        items(installedPlugins, key = { it.id }) { plugin ->
                            InstalledPluginCard(
                                plugin = plugin,
                                onToggle = { viewModel.togglePlugin(plugin) },
                                onDelete = { viewModel.deletePlugin(plugin) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Modrinth Hub
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchInputText,
                        onValueChange = {
                            searchInputText = it
                            viewModel.searchModrinth(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modrinth_search_input"),
                        placeholder = { Text("Search Modrinth plugins (e.g. Geyser, Chunky, spark)...", fontSize = 12.sp, color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = PaperGreen, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchInputText.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchInputText = ""
                                    viewModel.searchModrinth("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.searchModrinth(searchInputText)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PaperConsoleBg,
                            unfocusedContainerColor = PaperConsoleBg,
                            focusedBorderColor = PaperGreen,
                            unfocusedBorderColor = PaperSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PaperGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips for Modrinth
                    val modrinthFilterCategories = listOf("All", "Paper", "Optimization", "Utility", "Management", "Crossplay", "World")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(modrinthFilterCategories) { cat ->
                            val isSelected = (cat == "All" && selectedCategoryFilter == null) || selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (cat == "All") null else cat
                                    val q = if (cat == "All") "" else cat.lowercase()
                                    searchInputText = q
                                    viewModel.searchModrinth(q)
                                },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PaperGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = PaperGreen
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSearchingModrinth) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PaperGreen, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Searching Modrinth plugin index...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    } else if (modrinthHits.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "None", tint = Color.Gray, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No plugins found matching \"$searchInputText\"", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "Modrinth Results (${modrinthHits.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PaperGreen
                                )
                            }

                            items(modrinthHits, key = { it.projectId }) { hit ->
                                val isInstalled = installedPlugins.any {
                                    it.pluginId.equals(hit.projectId, ignoreCase = true) ||
                                    it.name.equals(hit.title, ignoreCase = true)
                                }
                                ModrinthPluginCard(
                                    hit = hit,
                                    isInstalled = isInstalled,
                                    onInstall = {
                                        viewModel.installModrinthPlugin(hit) {
                                            Toast.makeText(context, "Installed ${hit.title} from Modrinth!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Curated Catalog / Store
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Curated PaperMC Essentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PaperCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Essential plugins tuned for running smoothly on Android devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    items(curatedCatalog, key = { it.pluginId }) { item ->
                        val isAlreadyInstalled = installedPlugins.any { it.pluginId == item.pluginId || it.name.equals(item.name, ignoreCase = true) }
                        CatalogPluginCard(
                            plugin = item,
                            isInstalled = isAlreadyInstalled,
                            onInstall = {
                                viewModel.installPlugin(
                                    pluginId = item.pluginId,
                                    name = item.name,
                                    version = item.version,
                                    author = item.author,
                                    description = item.description,
                                    category = item.category,
                                    fileName = item.fileName,
                                    source = "curated"
                                )
                                Toast.makeText(context, "Installed ${item.name}!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCustomPluginDialog) {
        AddCustomPluginDialog(
            onDismiss = { showCustomPluginDialog = false },
            onAdd = { name, author, desc ->
                viewModel.installPlugin(
                    pluginId = name.lowercase().replace(" ", "_"),
                    name = name,
                    version = "1.0.0",
                    author = author.ifBlank { "Custom" },
                    description = desc.ifBlank { "Custom plugin added manually" },
                    category = "Manual",
                    fileName = "${name.replace(" ", "")}.jar",
                    source = "manual"
                )
                showCustomPluginDialog = false
                Toast.makeText(context, "Added $name to server plugins!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ModrinthPluginCard(
    hit: ModrinthProjectHit,
    isInstalled: Boolean,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                if (!hit.iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(hit.iconUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = hit.title,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaperDarkBg),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaperGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Plugin", tint = PaperGreen, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hit.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        if (isInstalled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaperSurfaceElevated)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "Installed", tint = PaperGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Installed", color = PaperGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onInstall,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = PaperGreen,
                                    contentColor = PaperDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Install", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Install", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by ${hit.author ?: "Modrinth"} • ${hit.latestVersion ?: "Latest"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = hit.description ?: "Minecraft Paper plugin available on Modrinth.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Downloads & Follows stats
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = "Downloads", tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    val dlFormatted = formatDownloads(hit.downloads ?: 0)
                    Text(dlFormatted, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 11.sp)

                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Default.Star, contentDescription = "Follows", tint = PaperGold, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${hit.follows ?: 0}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 11.sp)
                }

                // Category badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    hit.categories?.take(2)?.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PaperSurfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = PaperCyan, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

fun formatDownloads(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
        else -> "$count"
    }
}

@Composable
fun InstalledPluginCard(
    plugin: InstalledPluginEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeBg, badgeTextColor, badgeLabel) = when (plugin.source) {
                        "modrinth" -> Triple(PaperGreen.copy(alpha = 0.2f), PaperGreen, "Modrinth")
                        "file" -> Triple(PaperPurple.copy(alpha = 0.2f), PaperPurple, "Local .jar")
                        else -> Triple(PaperCyan.copy(alpha = 0.15f), PaperCyan, plugin.category)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeTextColor,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${plugin.version}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = plugin.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PaperDarkBg,
                            checkedTrackColor = PaperGreen
                        )
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PaperRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Author: ${plugin.author}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                val sizeText = if (plugin.fileSizeBytes > 0) FileUtils.formatBytes(plugin.fileSizeBytes) else plugin.fileName
                Text(
                    text = "${plugin.fileName} ($sizeText)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun CatalogPluginCard(
    plugin: CatalogPlugin,
    isInstalled: Boolean,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaperGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = plugin.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PaperGreen,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = plugin.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by ${plugin.author} • v${plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaperSurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "Installed", tint = PaperGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Installed", color = PaperGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onInstall,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PaperCyan,
                            contentColor = PaperDarkBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Install", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun AddCustomPluginDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, author: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Plugin Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plugin Name (e.g. MyCustomPlugin)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, author, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
