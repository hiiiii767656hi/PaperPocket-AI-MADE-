package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.server.ServerStatus
import com.example.ui.MainViewModel
import com.example.ui.components.ImportServerZipDialog
import com.example.ui.components.ServerProfileDialog
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.ConsoleScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlayersScreen
import com.example.ui.screens.PluginsScreen
import com.example.ui.screens.ServerPropertiesScreen
import com.example.ui.screens.WorldsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGold
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import com.example.ui.theme.PaperSurfaceElevated

enum class AppDestination(val title: String) {
    DASHBOARD("Dashboard"),
    CONSOLE("Console"),
    CONFIG("Config"),
    PLUGINS("Plugins"),
    PLAYERS("Players"),
    WORLDS("Worlds & Backups"),
    SERVER_PROPERTIES("server.properties")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()
    val allServers by viewModel.allServers.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val paperVersions by viewModel.paperVersions.collectAsStateWithLifecycle()

    // ZIP Server Import States
    val zipAnalysis by viewModel.zipAnalysis.collectAsStateWithLifecycle()
    val isAnalyzingZip by viewModel.isAnalyzingZip.collectAsStateWithLifecycle()
    val isExtractingZip by viewModel.isExtractingZip.collectAsStateWithLifecycle()
    val zipExtractionProgress by viewModel.zipExtractionProgress.collectAsStateWithLifecycle()
    val zipExtractionStatus by viewModel.zipExtractionStatus.collectAsStateWithLifecycle()
    val importErrorMessage by viewModel.importErrorMessage.collectAsStateWithLifecycle()

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.analyzeZipForImport(it)
        }
    }

    // Request POST_NOTIFICATIONS on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentDestination != AppDestination.SERVER_PROPERTIES) {
                CenterAlignedTopAppBar(
                    title = {
                        // Server profile switcher chip in top app bar
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaperSurfaceElevated)
                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(12.dp))
                                .clickable { showProfileDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("server_profile_selector"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusDotColor = when (serverStatus) {
                                ServerStatus.RUNNING -> PaperGreen
                                ServerStatus.STARTING -> PaperGold
                                ServerStatus.STOPPING -> PaperRed
                                ServerStatus.STOPPED -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentServer?.name ?: "Paper Server",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Server",
                                tint = PaperCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { currentDestination = AppDestination.WORLDS },
                            modifier = Modifier.testTag("worlds_nav_button")
                        ) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = "Worlds & Backups",
                                tint = if (currentDestination == AppDestination.WORLDS) PaperCyan else Color.LightGray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = PaperSurface,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = PaperSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                val navItems = listOf(
                    Triple(AppDestination.DASHBOARD, Icons.Default.Dashboard, "Home"),
                    Triple(AppDestination.CONSOLE, Icons.Default.Terminal, "Console"),
                    Triple(AppDestination.CONFIG, Icons.Default.Tune, "Config"),
                    Triple(AppDestination.PLUGINS, Icons.Default.Extension, "Plugins"),
                    Triple(AppDestination.PLAYERS, Icons.Default.People, "Players")
                )

                navItems.forEach { (dest, icon, label) ->
                    val isSelected = currentDestination == dest || (dest == AppDestination.CONFIG && currentDestination == AppDestination.SERVER_PROPERTIES)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = dest },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaperDarkBg,
                            selectedTextColor = PaperCyan,
                            indicatorColor = PaperCyan,
                            unselectedIconColor = Color(0xFF78909C),
                            unselectedTextColor = Color(0xFF78909C)
                        ),
                        modifier = Modifier.testTag("nav_item_${label.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PaperDarkBg)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { dest ->
                when (dest) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToConsole = { currentDestination = AppDestination.CONSOLE }
                    )
                    AppDestination.CONSOLE -> ConsoleScreen(viewModel = viewModel)
                    AppDestination.CONFIG -> ConfigScreen(
                        viewModel = viewModel,
                        onNavigateToProperties = { currentDestination = AppDestination.SERVER_PROPERTIES }
                    )
                    AppDestination.PLUGINS -> PluginsScreen(viewModel = viewModel)
                    AppDestination.PLAYERS -> PlayersScreen(viewModel = viewModel)
                    AppDestination.WORLDS -> WorldsScreen(viewModel = viewModel)
                    AppDestination.SERVER_PROPERTIES -> ServerPropertiesScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = AppDestination.CONFIG }
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        ServerProfileDialog(
            servers = allServers,
            activeServer = currentServer,
            availableVersions = paperVersions,
            onSelectServer = { viewModel.selectServer(it) },
            onCreateServer = { name, version, port, motd, gm, diff, ram ->
                viewModel.createServerProfile(name, version, port, motd, gm, diff, ram)
            },
            onImportZip = {
                zipPickerLauncher.launch("*/*")
            },
            onDeleteServer = { viewModel.deleteCurrentServer() },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (zipAnalysis != null || isAnalyzingZip || isExtractingZip || importErrorMessage != null) {
        ImportServerZipDialog(
            analysis = zipAnalysis,
            isAnalyzing = isAnalyzingZip,
            isExtracting = isExtractingZip,
            extractionProgress = zipExtractionProgress,
            extractionStatus = zipExtractionStatus,
            errorMessage = importErrorMessage,
            onConfirmImport = { options ->
                viewModel.confirmImportZip(options) { created ->
                    Toast.makeText(context, "Server '${created.name}' imported & adapted from start.bat!", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { viewModel.dismissZipImport() }
        )
    }
}
