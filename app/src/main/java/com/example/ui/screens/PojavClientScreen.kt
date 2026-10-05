package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.server.HotbarItem
import com.example.server.PojavChatMessage
import com.example.server.ServerStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGold
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperPurple
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import com.example.ui.theme.PaperSurfaceElevated
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val HOTBAR_ITEMS = listOf(
    HotbarItem("diamond_sword", "Diamond Sword", "⚔️", 1),
    HotbarItem("diamond_pickaxe", "Diamond Pickaxe", "⛏️", 1),
    HotbarItem("grass_block", "Grass Block", "🟩", 64),
    HotbarItem("oak_planks", "Oak Planks", "🪵", 64),
    HotbarItem("cobblestone", "Cobblestone", "🧱", 64),
    HotbarItem("torch", "Torch", "🔥", 32),
    HotbarItem("golden_apple", "Golden Apple", "🍏", 16),
    HotbarItem("bread", "Bread", "🍞", 64),
    HotbarItem("water_bucket", "Water Bucket", "🪣", 1)
)

private val ALL_INVENTORY_ITEMS = listOf(
    HotbarItem("diamond_sword", "Diamond Sword", "⚔️", 1),
    HotbarItem("diamond_pickaxe", "Diamond Pickaxe", "⛏️", 1),
    HotbarItem("diamond_axe", "Diamond Axe", "🪓", 1),
    HotbarItem("diamond_shovel", "Diamond Shovel", "🗡️", 1),
    HotbarItem("bow", "Bow", "🏹", 1),
    HotbarItem("arrow", "Arrow", "🎯", 64),
    HotbarItem("grass_block", "Grass Block", "🟩", 64),
    HotbarItem("dirt", "Dirt", "🟫", 64),
    HotbarItem("stone", "Stone", "🪨", 64),
    HotbarItem("oak_planks", "Oak Planks", "🪵", 64),
    HotbarItem("cobblestone", "Cobblestone", "🧱", 64),
    HotbarItem("glass", "Glass", "🪟", 64),
    HotbarItem("torch", "Torch", "🔥", 64),
    HotbarItem("lantern", "Lantern", "🏮", 16),
    HotbarItem("redstone", "Redstone Dust", "🔴", 64),
    HotbarItem("tnt", "TNT", "🧨", 64),
    HotbarItem("golden_apple", "Golden Apple", "🍏", 64),
    HotbarItem("enchanted_apple", "Enchanted Apple", "✨", 8),
    HotbarItem("bread", "Bread", "🍞", 64),
    HotbarItem("cooked_beef", "Cooked Steak", "🥩", 64),
    HotbarItem("diamond", "Diamond", "💎", 64),
    HotbarItem("emerald", "Emerald", "❇️", 64),
    HotbarItem("iron_ingot", "Iron Ingot", "⚙️", 64),
    HotbarItem("gold_ingot", "Gold Ingot", "🪙", 64),
    HotbarItem("water_bucket", "Water Bucket", "🪣", 1),
    HotbarItem("lava_bucket", "Lava Bucket", "🌋", 1),
    HotbarItem("ender_pearl", "Ender Pearl", "🔮", 16)
)

@Composable
fun PojavClientScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val pojavState by viewModel.pojavState.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }
    var showEditAccountDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (pojavState.isRunning) {
            viewModel.stopPojavClient()
        } else {
            onNavigateBack()
        }
    }

    if (pojavState.isRunning) {
        // In-Game Pojav Simulated Viewport & Touch Controls
        PojavInGameViewport(
            viewModel = viewModel,
            onExit = { viewModel.stopPojavClient() },
            onOpenInventory = { showInventoryDialog = true },
            onOpenSettings = { showSettingsDialog = true }
        )
    } else {
        // Pojav Launcher Home Menu (Minecraft 1.21.1 Title Screen & Test Launcher)
        PojavLauncherHomeMenu(
            viewModel = viewModel,
            currentServer = currentServer,
            serverStatus = serverStatus,
            onLaunchDirect = { connectServer ->
                viewModel.launchPojavClient(connectToServer = connectServer)
            },
            onOpenSettings = { showSettingsDialog = true },
            onOpenAccountEdit = { showEditAccountDialog = true },
            onNavigateBack = onNavigateBack
        )
    }

    if (showSettingsDialog) {
        PojavSettingsDialog(
            currentSettings = pojavState,
            onSave = { version, ram, renderer, user ->
                viewModel.updatePojavSettings(version, ram, renderer, user)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showInventoryDialog) {
        PojavInventoryDialog(
            selectedSlot = pojavState.selectedHotbarSlot,
            onSelectSlot = { slot -> viewModel.selectPojavHotbarSlot(slot) },
            onDismiss = { showInventoryDialog = false }
        )
    }

    if (showEditAccountDialog) {
        PojavAccountDialog(
            currentUsername = pojavState.username,
            onSave = { newName ->
                viewModel.updatePojavSettings(
                    version = pojavState.minecraftVersion,
                    ramMb = pojavState.allocatedRamMb,
                    renderer = pojavState.renderer,
                    username = newName
                )
                showEditAccountDialog = false
            },
            onDismiss = { showEditAccountDialog = false }
        )
    }
}

@Composable
private fun PojavLauncherHomeMenu(
    viewModel: MainViewModel,
    currentServer: com.example.data.local.entity.ServerProfileEntity?,
    serverStatus: ServerStatus,
    onLaunchDirect: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccountEdit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val pojavState by viewModel.pojavState.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimation")
    val splashScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pojav_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pojav Core Test Menu",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("pojav_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = PaperCyan
                        )
                    }
                }
            }
        }

        item {
            // Hero Pojav Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PaperSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pojav_banner),
                        contentDescription = "Pojav 1.21.1 Core Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xCC0B1118),
                                        Color(0xF00B1118)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaperPurple)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "POJAV CORE v3.3",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaperCyan.copy(alpha = 0.2f))
                                    .border(1.dp, PaperCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "MINECRAFT 1.21.1",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaperCyan
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Java Edition In-App Test Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            // Title Screen Box with Minecraft logo style
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "MINECRAFT",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFC6C6C6),
                            modifier = Modifier.offset(y = 2.dp)
                        )
                        Text(
                            text = "MINECRAFT",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "JAVA EDITION 1.21.1",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PaperCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "* Built-in Pojav Core for PaperHost! *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaperGold,
                        modifier = Modifier
                            .scale(splashScale)
                            .rotate(-4f)
                    )
                }
            }
        }

        item {
            // Profile & Target Server Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PaperSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaperCyan.copy(alpha = 0.2f))
                                    .border(1.dp, PaperCyan, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🕹️", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = pojavState.username,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Offline Java Player • JDK 21",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenAccountEdit,
                            modifier = Modifier.testTag("pojav_edit_account_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Account", tint = PaperCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PaperSurfaceBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local Paper Server",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "127.0.0.1:${currentServer?.port ?: 25565}",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        val (statusText, statusColor) = when (serverStatus) {
                            ServerStatus.RUNNING -> Pair("ONLINE", PaperGreen)
                            ServerStatus.STARTING -> Pair("STARTING", PaperGold)
                            ServerStatus.STOPPING -> Pair("STOPPING", PaperRed)
                            ServerStatus.STOPPED -> Pair("OFFLINE", Color.Gray)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Main Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onLaunchDirect(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pojav_join_server_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaperGreen,
                        contentColor = PaperDarkBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MULTIPLAYER • CONNECT TO LOCALHOST",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { onLaunchDirect(false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pojav_singleplayer_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaperSurfaceElevated,
                        contentColor = PaperCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SINGLEPLAYER • TEST 3D WORLD DEMO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PojavInGameViewport(
    viewModel: MainViewModel,
    onExit: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val pojavState by viewModel.pojavState.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    var chatInputText by remember { mutableStateOf("") }
    var actionEffectCounter by remember { mutableIntStateOf(0) }
    var lastActionType by remember { mutableStateOf("") }

    var playerYaw by remember { mutableFloatStateOf(pojavState.yaw) }
    var playerPitch by remember { mutableFloatStateOf(pojavState.pitch) }
    var playerX by remember { mutableStateOf(pojavState.posX) }
    var playerY by remember { mutableStateOf(pojavState.posY) }
    var playerZ by remember { mutableStateOf(pojavState.posZ) }

    fun triggerHaptic() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 3D Canvas Voxel Viewport
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        playerYaw = (playerYaw + dragAmount.x * 0.4f) % 360f
                        playerPitch = (playerPitch + dragAmount.y * 0.4f).coerceIn(-89f, 89f)
                        viewModel.updatePojavPosition(playerX, playerY, playerZ, playerYaw, playerPitch)
                    }
                }
        ) {
            drawMinecraft3dWorld(
                yaw = playerYaw,
                pitch = playerPitch,
                playerX = playerX,
                playerY = playerY,
                playerZ = playerZ,
                heldItem = HOTBAR_ITEMS.getOrNull(pojavState.selectedHotbarSlot)?.displayName ?: "Hand",
                actionCounter = actionEffectCounter,
                actionType = lastActionType
            )
        }

        // Top Controls Bar (Classic PojavLauncher Overlay Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PojavTopButton("EXIT", onClick = onExit)
                PojavTopButton("F3", isActive = pojavState.isF3Open, onClick = { viewModel.togglePojavF3() })
                PojavTopButton("CHAT", isActive = pojavState.isChatOpen, onClick = { viewModel.togglePojavChat() })
                PojavTopButton("INV", onClick = onOpenInventory)
            }

            // Connection indicator badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (pojavState.isConnectedToServer) PaperGreen else PaperGold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pojavState.isConnectedToServer) "SERVER: 127.0.0.1:25565" else "LOCAL WORLD",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PojavTopButton("GUI", isActive = showControls, onClick = { showControls = !showControls })
                PojavTopButton("PRI", onClick = {
                    triggerHaptic()
                    actionEffectCounter++
                    lastActionType = "ATTACK"
                    Toast.makeText(context, "Mined / Attacked with ${HOTBAR_ITEMS[pojavState.selectedHotbarSlot].displayName}!", Toast.LENGTH_SHORT).show()
                })
                PojavTopButton("SEC", onClick = {
                    triggerHaptic()
                    actionEffectCounter++
                    lastActionType = "USE"
                    Toast.makeText(context, "Placed / Used ${HOTBAR_ITEMS[pojavState.selectedHotbarSlot].displayName}!", Toast.LENGTH_SHORT).show()
                })
                PojavTopButton("OPT", onClick = onOpenSettings)
            }
        }

        // F3 Debug Screen (Authentic Minecraft Java 1.21.1 Replica)
        if (pojavState.isF3Open) {
            PojavF3DebugOverlay(
                pojavState = pojavState,
                yaw = playerYaw,
                pitch = playerPitch,
                x = playerX,
                y = playerY,
                z = playerZ
            )
        }

        // Crosshair (+)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val stroke = 2f
                drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = stroke)
                drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = stroke)
            }
        }

        // Bottom Minecraft HUD (Health, Hunger, Level, Hotbar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hearts & Hunger Bars
            Row(
                modifier = Modifier
                    .width(320.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 10 Hearts
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(10) {
                        Text(text = "❤️", fontSize = 11.sp)
                    }
                }
                // 10 Drumsticks
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(10) {
                        Text(text = "🍗", fontSize = 11.sp)
                    }
                }
            }

            // Experience Level Bar
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2E7D32))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(6.dp)
                        .background(Color(0xFF81C784))
                )
            }
            Text(
                text = "${pojavState.level}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF81C784)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 9-Slot Hotbar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC1E1E1E))
                    .border(2.dp, Color(0xFF8D8D8D), RoundedCornerShape(6.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                HOTBAR_ITEMS.forEachIndexed { index, item ->
                    val isSelected = index == pojavState.selectedHotbarSlot
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) Color(0xFF424242) else Color(0x66000000))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0xFF555555),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                triggerHaptic()
                                viewModel.selectPojavHotbarSlot(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.iconEmoji, fontSize = 16.sp)
                        if (item.count > 1) {
                            Text(
                                text = "${item.count}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 2.dp, bottom = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // On-Screen Touch Controls (Pojav D-Pad & Action Buttons)
        if (showControls) {
            // Left D-Pad (W, A, S, D, Sprint, Shift)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 40.dp)
                    .size(130.dp)
            ) {
                // Forward (W)
                PojavDPadButton(
                    label = "W",
                    modifier = Modifier.align(Alignment.TopCenter),
                    onClick = {
                        triggerHaptic()
                        val rad = Math.toRadians(playerYaw.toDouble())
                        playerX += sin(rad) * 0.5
                        playerZ += cos(rad) * 0.5
                        viewModel.updatePojavPosition(playerX, playerY, playerZ, playerYaw, playerPitch)
                    }
                )
                // Backward (S)
                PojavDPadButton(
                    label = "S",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClick = {
                        triggerHaptic()
                        val rad = Math.toRadians(playerYaw.toDouble())
                        playerX -= sin(rad) * 0.5
                        playerZ -= cos(rad) * 0.5
                        viewModel.updatePojavPosition(playerX, playerY, playerZ, playerYaw, playerPitch)
                    }
                )
                // Left (A)
                PojavDPadButton(
                    label = "A",
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = {
                        triggerHaptic()
                        val rad = Math.toRadians((playerYaw - 90).toDouble())
                        playerX += sin(rad) * 0.5
                        playerZ += cos(rad) * 0.5
                        viewModel.updatePojavPosition(playerX, playerY, playerZ, playerYaw, playerPitch)
                    }
                )
                // Right (D)
                PojavDPadButton(
                    label = "D",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = {
                        triggerHaptic()
                        val rad = Math.toRadians((playerYaw + 90).toDouble())
                        playerX += sin(rad) * 0.5
                        playerZ += cos(rad) * 0.5
                        viewModel.updatePojavPosition(playerX, playerY, playerZ, playerYaw, playerPitch)
                    }
                )
                // Center Sprint / Sneak toggle
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x882A2A2A))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            triggerHaptic()
                            Toast.makeText(context, "Sprint Activated!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "RUN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Right Action Controls (JUMP)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x88333333))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                            triggerHaptic()
                            playerY = (playerY + 1.0).coerceAtMost(80.0)
                            viewModel.updatePojavPosition(playerX, playerY, playerZ, playerYaw, playerPitch)
                            // return back down after delay
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "JUMP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Live Chat Feed & Input Overlay
        if (pojavState.isChatOpen) {
            PojavChatOverlay(
                messages = pojavState.chatMessages,
                inputText = chatInputText,
                onInputChange = { chatInputText = it },
                onSend = {
                    if (chatInputText.isNotBlank()) {
                        viewModel.sendPojavChatMessage(chatInputText)
                        chatInputText = ""
                    }
                },
                onQuickCommand = { cmd ->
                    viewModel.sendPojavChatMessage(cmd)
                },
                onClose = { viewModel.togglePojavChat() }
            )
        }
    }
}

private fun DrawScope.drawMinecraft3dWorld(
    yaw: Float,
    pitch: Float,
    playerX: Double,
    playerY: Double,
    playerZ: Double,
    heldItem: String,
    actionCounter: Int,
    actionType: String
) {
    val width = size.width
    val height = size.height
    val horizonY = height * 0.45f + (pitch * 1.8f)

    // 1. Sky & Sun/Clouds
    val skyColor = Color(0xFF64B5F6)
    val horizonColor = Color(0xFFBBDEFB)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyColor, horizonColor),
            startY = 0f,
            endY = horizonY.coerceAtLeast(10f)
        ),
        topLeft = Offset(0f, 0f),
        size = Size(width, horizonY.coerceAtLeast(0f))
    )

    // Sun
    val sunOffset = (yaw % 360) / 360f * width
    drawRect(
        color = Color(0xFFFFF9C4),
        topLeft = Offset((sunOffset + 100f) % width, horizonY * 0.3f),
        size = Size(40f, 40f)
    )

    // Pixel Clouds
    drawRect(
        color = Color(0xCCFFFFFF),
        topLeft = Offset((sunOffset * 0.5f) % width, horizonY * 0.15f),
        size = Size(120f, 25f)
    )
    drawRect(
        color = Color(0xCCFFFFFF),
        topLeft = Offset((sunOffset * 0.5f + 160f) % width, horizonY * 0.22f),
        size = Size(160f, 30f)
    )

    // 2. Ground & Perspective Grass Blocks
    val groundColor = Color(0xFF4CAF50) // Grass green
    val dirtColor = Color(0xFF5D4037)  // Dirt brown
    val stoneColor = Color(0xFF757575) // Stone gray

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(groundColor, Color(0xFF388E3C), dirtColor),
            startY = horizonY,
            endY = height
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, height - horizonY)
    )

    // Draw Voxel Grid Lines to simulate 3D blocks perspective
    val gridLines = 8
    for (i in 1..gridLines) {
        val y = horizonY + (height - horizonY) * (i.toFloat() / gridLines) * (i.toFloat() / gridLines)
        drawLine(
            color = Color(0x33000000),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 2f
        )
    }

    // Draw Perspective Spawn Structure (Cobblestone Pillar & Torch)
    val centerX = width / 2
    val pillarBaseY = horizonY + (height - horizonY) * 0.6f
    val pillarTopY = pillarBaseY - 90f

    // Cobblestone pillar
    drawRect(
        color = Color(0xFF616161),
        topLeft = Offset(centerX - 35f, pillarTopY),
        size = Size(70f, pillarBaseY - pillarTopY)
    )
    drawRect(
        color = Color(0xFF424242),
        topLeft = Offset(centerX - 30f, pillarTopY + 5f),
        size = Size(60f, pillarBaseY - pillarTopY - 10f),
        style = Stroke(width = 3f)
    )

    // Torch on pillar
    drawRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(centerX - 4f, pillarTopY - 24f),
        size = Size(8f, 24f)
    )
    // Torch Flame
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 8f,
        center = Offset(centerX, pillarTopY - 28f)
    )
    drawCircle(
        color = Color(0xFFFF5722),
        radius = 4f,
        center = Offset(centerX, pillarTopY - 28f)
    )

    // Red Flower in grass
    val flowerX = centerX + 120f
    val flowerY = horizonY + 110f
    drawLine(Color(0xFF2E7D32), Offset(flowerX, flowerY), Offset(flowerX, flowerY - 20f), strokeWidth = 3f)
    drawCircle(Color(0xFFE53935), radius = 7f, center = Offset(flowerX, flowerY - 22f))
    drawCircle(Color(0xFFFFEB3B), radius = 3f, center = Offset(flowerX, flowerY - 22f))

    // 3. Held Item in Bottom-Right Corner (Diamond Sword / Pickaxe / Block)
    val itemBaseX = width * 0.72f
    val itemBaseY = height * 0.75f
    val actionAnim = if (actionCounter > 0) sin(actionCounter.toFloat() * 1.5f) * 20f else 0f

    // Player Hand & Item
    drawRect(
        color = Color(0xFFD7CCC8), // Arm skin tone
        topLeft = Offset(itemBaseX + 60f, itemBaseY + 40f + actionAnim),
        size = Size(80f, 160f)
    )
    // Cyan Diamond Blade / Tool
    val itemColor = when {
        heldItem.contains("Sword") || heldItem.contains("Pickaxe") -> Color(0xFF00E5FF)
        heldItem.contains("Grass") -> Color(0xFF4CAF50)
        heldItem.contains("Planks") -> Color(0xFFBCAAA4)
        else -> Color(0xFFFFD54F)
    }

    drawRect(
        color = itemColor,
        topLeft = Offset(itemBaseX, itemBaseY - 40f + actionAnim),
        size = Size(40f, 90f)
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(itemBaseX + 4f, itemBaseY - 36f + actionAnim),
        size = Size(8f, 70f)
    )

    // Action Impact Particles
    if (actionCounter > 0) {
        val pColor = if (actionType == "ATTACK") Color.White else Color(0xFF81C784)
        drawCircle(pColor, radius = 6f, center = Offset(width / 2 + 10f, height / 2 - 15f))
        drawCircle(pColor, radius = 4f, center = Offset(width / 2 - 12f, height / 2 + 10f))
        drawCircle(pColor, radius = 5f, center = Offset(width / 2 + 18f, height / 2 + 8f))
    }
}

@Composable
private fun PojavTopButton(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) PaperCyan else Color(0x991E1E1E))
            .border(1.dp, if (isActive) Color.White else Color(0x88FFFFFF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isActive) PaperDarkBg else Color.White
        )
    }
}

@Composable
private fun PojavDPadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x88212121))
            .border(1.5.dp, Color(0xAAFFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

@Composable
private fun PojavF3DebugOverlay(
    pojavState: com.example.server.PojavClientState,
    yaw: Float,
    pitch: Float,
    x: Double,
    y: Double,
    z: Double
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, start = 8.dp, end = 8.dp)
    ) {
        // Left Column
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color(0x88000000))
                .padding(6.dp)
        ) {
            val facing = when (((yaw % 360 + 360) % 360).toInt()) {
                in 45..134 -> "west (Towards negative X)"
                in 135..224 -> "north (Towards negative Z)"
                in 225..314 -> "east (Towards positive X)"
                else -> "south (Towards positive Z)"
            }

            DebugText("Minecraft 1.21.1 (1.21.1/vanilla - Pojav Core v3.3)")
            DebugText("60 fps T: 60, B: 0")
            DebugText("Server: Paper 1.21.1 (${if (pojavState.isConnectedToServer) "Connected 127.0.0.1:25565" else "Singleplayer Demo"})")
            DebugText("XYZ: ${String.format("%.3f / %.3f / %.3f", x, y, z)}")
            DebugText("Block: ${x.toInt()} ${y.toInt()} ${z.toInt()} [${(x.toInt() and 15)} ${(y.toInt() and 15)} ${(z.toInt() and 15)}]")
            DebugText("Chunk: ${(x.toInt() shr 4)} ${(y.toInt() shr 4)} ${(z.toInt() shr 4)}")
            DebugText("Facing: $facing (${String.format("%.1f / %.1f", yaw, pitch)})")
            DebugText("Client Light: 15 (15 sky, 0 block)")
            DebugText("Biome: minecraft:plains")
            DebugText("Difficulty: 1 (Easy), Gamemode: ${pojavState.gamemode}")
        }

        // Right Column
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0x88000000))
                .padding(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            DebugText("Java: 21.0.12 64bit (Pojav OpenJDK)")
            DebugText("Mem: 24% 492/${pojavState.allocatedRamMb}MB")
            DebugText("Allocated: 100% ${pojavState.allocatedRamMb}MB")
            DebugText("CPU: 8x ARM64-v8a")
            DebugText("Display: 1080x2400 (${pojavState.renderer})")
            DebugText("Pojav Bridge: Active (Native JNI)")
        }
    }
}

@Composable
private fun DebugText(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFE0E0E0),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PojavChatOverlay(
    messages: List<PojavChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickCommand: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Chat Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Minecraft 1.21.1 In-Game Chat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = Color.White)
                }
            }

            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "§7[No messages yet. Send a message or command below!]",
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
                items(messages) { msg ->
                    Text(
                        text = if (msg.isSystem) msg.text else "<${msg.sender}> ${msg.text}",
                        color = if (msg.isSystem) PaperGold else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick command chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickCommandChip("/help", onQuickCommand)
                QuickCommandChip("/gamemode creative", onQuickCommand)
                QuickCommandChip("/time set day", onQuickCommand)
                QuickCommandChip("/tp 0 65 0", onQuickCommand)
            }

            // Chat input bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Type chat or /command...", fontSize = 12.sp, color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pojav_chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PaperCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = Color(0xDD1E1E1E),
                        unfocusedContainerColor = Color(0xDD1E1E1E)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaperCyan)
                        .testTag("pojav_chat_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PaperDarkBg)
                }
            }
        }
    }
}

@Composable
private fun QuickCommandChip(label: String, onExecute: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF263238))
            .border(1.dp, PaperCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onExecute(label) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = PaperCyan, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PojavInventoryDialog(
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF8D8D8D), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crafting & Inventory (27 Slots)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 27-slot grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rows = ALL_INVENTORY_ITEMS.chunked(9)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .border(1.dp, Color(0xFF4A4A4A), RoundedCornerShape(4.dp))
                                        .clickable {
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.iconEmoji, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
                ) {
                    Text("Close Inventory", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PojavSettingsDialog(
    currentSettings: com.example.server.PojavClientState,
    onSave: (String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var version by remember { mutableStateOf(currentSettings.minecraftVersion) }
    var ramMb by remember { mutableFloatStateOf(currentSettings.allocatedRamMb.toFloat()) }
    var renderer by remember { mutableStateOf(currentSettings.renderer) }
    var username by remember { mutableStateOf(currentSettings.username) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PaperSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "⚙️ Pojav Core Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Offline Player Username") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Version
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Minecraft Java Version") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // RAM Slider
                Column {
                    Text(
                        text = "Allocated RAM: ${ramMb.toInt()} MB",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Slider(
                        value = ramMb,
                        onValueChange = { ramMb = it },
                        valueRange = 1024f..4096f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = PaperCyan,
                            activeTrackColor = PaperCyan
                        )
                    )
                }

                // Renderer Selection
                Column {
                    Text("Renderer Backend:", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = renderer,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PaperCyan
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(version, ramMb.toInt(), renderer, username) },
                        colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
                    ) {
                        Text("Save & Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PojavAccountDialog(
    currentUsername: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentUsername) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PaperSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "👤 Switch Pojav Player",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { name = "Steve" },
                        colors = ButtonDefaults.buttonColors(containerColor = PaperSurfaceElevated)
                    ) {
                        Text("Steve", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { name = "Alex" },
                        colors = ButtonDefaults.buttonColors(containerColor = PaperSurfaceElevated)
                    ) {
                        Text("Alex", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { name = "PojavTester" },
                        colors = ButtonDefaults.buttonColors(containerColor = PaperSurfaceElevated)
                    ) {
                        Text("PojavTester", fontSize = 11.sp, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name) },
                        colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
