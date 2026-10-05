package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ServerProfileEntity
import com.example.server.PlayitTunnelInfo
import com.example.server.PlayitTunnelStatus
import com.example.server.ServerMetrics
import com.example.server.ServerStatus
import com.example.ui.MainViewModel
import com.example.ui.components.MinecraftText
import com.example.ui.components.ResourceMeterRow
import com.example.ui.components.TpsRadialGauge
import com.example.ui.components.UnknownHostHelpDialog
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

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToConsole: () -> Unit,
    onNavigateToFiles: () -> Unit = {},
    onNavigateToProperties: () -> Unit = {},
    onNavigateToPojav: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val server by viewModel.currentServer.collectAsStateWithLifecycle()
    val status by viewModel.serverStatus.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val deviceIp by viewModel.deviceIp.collectAsStateWithLifecycle()
    val runningServerId by viewModel.runningServerId.collectAsStateWithLifecycle()
    val runningServer by viewModel.runningServer.collectAsStateWithLifecycle()
    val playitTunnelInfo by viewModel.playitTunnelInfo.collectAsStateWithLifecycle()

    val isThisServerRunning = server != null && runningServerId == server?.id && status != ServerStatus.STOPPED
    val otherServerRunning = if (!isThisServerRunning && status != ServerStatus.STOPPED) runningServer else null
    val effectiveStatus = if (isThisServerRunning) status else ServerStatus.STOPPED

    var showConnectionDialog by remember { mutableStateOf(false) }
    var showUnknownHostDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            ServerHeaderCard(
                server = server,
                status = effectiveStatus,
                isRunning = isThisServerRunning,
                otherServerRunning = otherServerRunning,
                onStart = { server?.let { viewModel.startServer(it.id) } },
                onStop = { viewModel.stopServer() },
                onRestart = { viewModel.restartServer() },
                onOpenFiles = onNavigateToFiles
            )
        }

        item {
            ConnectionCard(
                deviceIp = deviceIp,
                port = server?.port ?: 25565,
                status = effectiveStatus,
                enableBedrock = server?.enableBedrockPing ?: true,
                onCopy = {
                    val fullAddress = "$deviceIp:${server?.port ?: 25565}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Server Address", fullAddress))
                    Toast.makeText(context, "Copied $fullAddress to clipboard!", Toast.LENGTH_SHORT).show()
                },
                onHelp = { showConnectionDialog = true }
            )
        }

        item {
            PojavTestClientCard(onLaunch = onNavigateToPojav)
        }

        item {
            PlayitDashboardCard(
                tunnelInfo = playitTunnelInfo,
                deviceIp = deviceIp,
                serverPort = server?.port ?: 25565,
                onToggle = { enable ->
                    viewModel.togglePlayitTunnel(enable)
                    if (enable) {
                        Toast.makeText(context, "Playit.gg tunnel starting...", Toast.LENGTH_SHORT).show()
                    }
                },
                onCopy = { address ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Playit Address", address))
                    Toast.makeText(context, "Copied $address! Share with friends to join.", Toast.LENGTH_SHORT).show()
                },
                onShare = { address ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Join my Minecraft Paper server from anywhere! Address: $address")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Server Address"))
                },
                onOpenUnknownHostGuide = { showUnknownHostDialog = true },
                onSaveAddress = { viewModel.updatePlayitPublicAddress(it) },
                onGenerateNewClaimLink = { viewModel.generateNewPlayitClaimLink() }
            )
        }

        item {
            PerformanceTelemetryCard(
                metrics = metrics,
                status = effectiveStatus
            )
        }

        item {
            QuickActionsCard(
                status = effectiveStatus,
                onExecute = { cmd -> viewModel.sendCommand(cmd) },
                onSimulateJoin = { viewModel.simulatePlayerJoin("Tester_${(10..99).random()}") },
                onOpenFiles = onNavigateToFiles,
                onOpenProperties = onNavigateToProperties
            )
        }

        item {
            LiveConsoleSnippetCard(
                logs = logs.takeLast(4),
                onOpenConsole = onNavigateToConsole
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConnectionDialog) {
        ConnectionGuideDialog(
            deviceIp = deviceIp,
            port = server?.port ?: 25565,
            onDismiss = { showConnectionDialog = false }
        )
    }

    if (showUnknownHostDialog) {
        UnknownHostHelpDialog(
            deviceIp = deviceIp,
            serverPort = server?.port ?: 25565,
            currentPlayitAddress = playitTunnelInfo.publicAddress,
            claimUrl = playitTunnelInfo.claimUrl,
            claimCode = playitTunnelInfo.claimCode,
            onSavePlayitAddress = { viewModel.updatePlayitPublicAddress(it) },
            onRefreshClaimCode = { viewModel.generateNewPlayitClaimLink() },
            onDismiss = { showUnknownHostDialog = false }
        )
    }
}

@Composable
fun ServerHeaderCard(
    server: ServerProfileEntity?,
    status: ServerStatus,
    isRunning: Boolean = false,
    otherServerRunning: ServerProfileEntity? = null,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onOpenFiles: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server?.name ?: "PaperMC Server",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (server?.importedFromZip == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PaperPurple.copy(alpha = 0.25f))
                                    .border(1.dp, PaperPurple.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ZIP IMPORT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = PaperPurple
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (server?.useCustomJar == true && !server?.customJarFileName.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PaperCyan.copy(alpha = 0.25f))
                                    .border(1.dp, PaperCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CUSTOM BUILD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = PaperCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = server?.customJarFileName ?: "custom.jar",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = "PaperMC ${server?.minecraftVersion ?: "1.21.1"} • Build #${server?.paperBuild ?: 120}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaperCyan
                            )
                        }
                    }
                }

                // Status Badge
                val (statusColor, statusLabel) = when (status) {
                    ServerStatus.RUNNING -> Pair(PaperGreen, "ONLINE")
                    ServerStatus.STARTING -> Pair(PaperGold, "STARTING")
                    ServerStatus.STOPPING -> Pair(PaperRed, "STOPPING")
                    ServerStatus.STOPPED -> Pair(Color.Gray, "OFFLINE")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                                .alpha(if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) pulseAlpha else 1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            MinecraftText(
                text = server?.motd ?: "§7A PaperMC Server on Phone",
                style = MaterialTheme.typography.bodySmall
            )

            // Working directory chip
            val dirPath = server?.getEffectiveDirectory(LocalContext.current)?.absolutePath ?: ""
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PaperDarkBg.copy(alpha = 0.6f))
                    .clickable { onOpenFiles() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PaperPurple, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dirPath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Files →", color = PaperPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            if (otherServerRunning != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PaperGold.copy(alpha = 0.15f))
                        .border(1.dp, PaperGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = PaperGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Other Server Online", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PaperGold)
                            Text(
                                "'${otherServerRunning.name}' (Port ${otherServerRunning.port}) is currently running. Starting '${server?.name ?: "this server"}' will switch active servers.",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (status) {
                    ServerStatus.STOPPED -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_server_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (otherServerRunning != null) PaperGreen else PaperGreen,
                                contentColor = PaperDarkBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Server")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (otherServerRunning != null) "Switch & Start '${server?.name ?: "Server"}'" else "Start Paper Server",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    ServerStatus.STARTING -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PaperGold,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Starting JVM & Paper...", fontWeight = FontWeight.Bold)
                        }
                    }
                    ServerStatus.RUNNING -> {
                        Button(
                            onClick = onStop,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stop_server_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaperRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Server")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("restart_server_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PaperSurfaceElevated,
                                contentColor = PaperCyan
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart Server")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart", fontWeight = FontWeight.Bold)
                        }
                    }
                    ServerStatus.STOPPING -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PaperRed,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Saving Chunks & Stopping...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    deviceIp: String,
    port: Int,
    status: ServerStatus,
    enableBedrock: Boolean,
    onCopy: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = "Wi-Fi LAN",
                        tint = PaperCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Network Connection",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                IconButton(onClick = onHelp, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Connection Guide",
                        tint = PaperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PaperDarkBg)
                    .border(1.dp, Color(0xFF263238), RoundedCornerShape(10.dp))
                    .clickable { onCopy() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JAVA & BEDROCK IP:PORT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        ),
                        color = Color.Gray
                    )
                    Text(
                        text = "$deviceIp:$port",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = PaperGreen
                    )
                }

                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy IP",
                    tint = PaperCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (enableBedrock) "● Bedrock LAN auto-discovery active" else "Java Edition only",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enableBedrock) PaperGreen else Color.Gray
                )
                Text(
                    text = if (status == ServerStatus.RUNNING) "Port open" else "Server stopped",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status == ServerStatus.RUNNING) PaperCyan else Color.Gray
                )
            }
        }
    }
}

@Composable
fun PlayitDashboardCard(
    tunnelInfo: PlayitTunnelInfo,
    deviceIp: String,
    serverPort: Int,
    onToggle: (Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenUnknownHostGuide: () -> Unit,
    onSaveAddress: (String) -> Unit,
    onGenerateNewClaimLink: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (tunnelInfo.isEnabled && tunnelInfo.isClaimed) PaperGreen.copy(alpha = 0.6f)
                else if (tunnelInfo.isEnabled) PaperGold.copy(alpha = 0.6f)
                else PaperSurfaceBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (tunnelInfo.isEnabled && tunnelInfo.isClaimed) PaperGreen.copy(alpha = 0.2f)
                                else if (tunnelInfo.isEnabled) PaperGold.copy(alpha = 0.2f)
                                else PaperDarkBg
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = "Playit.gg",
                            tint = if (tunnelInfo.isEnabled && tunnelInfo.isClaimed) PaperGreen else if (tunnelInfo.isEnabled) PaperGold else Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Playit.gg Public Tunnel",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            if (tunnelInfo.isEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                val (badgeBg, badgeText, badgeColor) = if (tunnelInfo.isClaimed) {
                                    Triple(PaperGreen.copy(alpha = 0.2f), "ONLINE • v1.1.0", PaperGreen)
                                } else {
                                    Triple(PaperGold.copy(alpha = 0.2f), "v1.1.0 ASSIGNABLE", PaperGold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        Text(
                            if (tunnelInfo.isEnabled && tunnelInfo.isClaimed) "Global internet access • Zero port forwarding"
                            else if (tunnelInfo.isEnabled) "Awaiting claimed address • Local Wi-Fi ready"
                            else "Allow friends outside home Wi-Fi to join",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Switch(
                    checked = tunnelInfo.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (tunnelInfo.isClaimed) PaperGreen else PaperGold,
                        checkedTrackColor = if (tunnelInfo.isClaimed) PaperGreen.copy(alpha = 0.4f) else PaperGold.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("dashboard_playit_toggle")
                )
            }

            if (tunnelInfo.isEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                if (tunnelInfo.isClaimed && tunnelInfo.publicAddress.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaperDarkBg)
                            .border(1.dp, PaperGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { onCopy(tunnelInfo.publicAddress) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GLOBAL PUBLIC PLAYIT ADDRESS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                                    color = Color.Gray
                                )
                                if (tunnelInfo.lastResolutionResult?.isResolvable == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• DNS VERIFIED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = PaperGreen
                                    )
                                }
                            }
                            Text(
                                text = tunnelInfo.publicAddress,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = PaperGreen,
                                maxLines = 1
                            )
                        }

                        Row {
                            IconButton(onClick = { onCopy(tunnelInfo.publicAddress) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Address", tint = PaperGreen, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onShare(tunnelInfo.publicAddress) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = PaperCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenUnknownHostGuide,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PaperSurfaceBorder)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = PaperGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Why 'Unknown Host'?", fontSize = 11.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onOpenUnknownHostGuide,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PaperCyan.copy(alpha = 0.5f))
                        ) {
                            Text("Change / Test Address", fontSize = 11.sp, color = PaperCyan)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaperDarkBg),
                        border = BorderStroke(1.dp, PaperGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = PaperGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Link Agent to Playit.gg Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PaperGold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "To play with friends over the internet, open your unique claim link below to connect this Android server to your Playit.gg account:",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val effectiveClaimUrl = if (tunnelInfo.claimUrl.isNotBlank()) tunnelInfo.claimUrl else if (tunnelInfo.claimCode.isNotBlank()) "https://playit.gg/claim/${tunnelInfo.claimCode}" else "https://playit.gg/claim"

                            // Claim URL display
                            Text("YOUR AGENT CLAIM LINK:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PaperGreen)
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaperSurface)
                                    .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionContainer(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = effectiveClaimUrl,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = PaperGreen,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Playit Claim URL", effectiveClaimUrl))
                                        Toast.makeText(context, "Copied Claim Link to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = PaperGreen, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (tunnelInfo.claimCode.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Claim Code: ", fontSize = 11.sp, color = Color.Gray)
                                    Text(tunnelInfo.claimCode, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PaperCyan)
                                }
                            }

                            if (tunnelInfo.claimStatusMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PaperGold, strokeWidth = 1.5.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tunnelInfo.claimStatusMessage, fontSize = 10.sp, color = PaperGold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveClaimUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Claim Link", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = onGenerateNewClaimLink,
                                    modifier = Modifier.height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "New Code", modifier = Modifier.size(14.dp), tint = Color.LightGray)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Fix 'version is too old v0.0.0' assistance card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaperPurple.copy(alpha = 0.12f))
                                    .border(1.dp, PaperPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = PaperPurple, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Playit says 'version too old v0.0.0'?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        "Playit.gg gives this warning if the agent was flagged as legacy self-managed. The app now registers as modern Assignable Agent v1.1.0 and auto-creates your Minecraft tunnel via API upon claim!",
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = onGenerateNewClaimLink,
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PaperPurple),
                                        border = BorderStroke(1.dp, PaperPurple.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Generate Fresh v1.1.0 Assignable Link", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Local Wi-Fi Direct IP Option
                            val directAddr = "$deviceIp:$serverPort"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaperSurface.copy(alpha = 0.6f))
                                    .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("LOCAL WI-FI (NO CLAIM NEEDED • NEVER UNKNOWN HOST)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PaperCyan)
                                    Text(directAddr, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PaperGreen)
                                }
                                IconButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Direct IP", directAddr))
                                        Toast.makeText(context, "Copied $directAddr!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PaperGreen, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onOpenUnknownHostGuide,
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PaperGold.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = PaperGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Why 'Unknown Host'? Guide & DNS Diagnostic", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = PaperGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceTelemetryCard(
    metrics: ServerMetrics,
    status: ServerStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                Text(
                    text = "Live Telemetry & Performance",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                val uptimeText = if (metrics.uptimeSeconds > 0) {
                    val hrs = metrics.uptimeSeconds / 3600
                    val mins = (metrics.uptimeSeconds % 3600) / 60
                    val secs = metrics.uptimeSeconds % 60
                    String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                } else "00:00:00"

                Text(
                    text = "Uptime: $uptimeText",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TpsRadialGauge(
                    tps = if (status == ServerStatus.RUNNING) metrics.tps else 0.0,
                    mspt = if (status == ServerStatus.RUNNING) metrics.mspt else 0.0
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val ramProg = (metrics.usedMemoryMb.toFloat() / metrics.allocatedMemoryMb.toFloat()).coerceIn(0f, 1f)
                    ResourceMeterRow(
                        label = "RAM Usage",
                        valueText = "${metrics.usedMemoryMb} / ${metrics.allocatedMemoryMb} MB",
                        progress = if (status == ServerStatus.RUNNING) ramProg else 0f,
                        barColor = if (ramProg > 0.85f) PaperRed else PaperCyan
                    )

                    val cpuProg = (metrics.cpuPercent / 100f).coerceIn(0f, 1f)
                    ResourceMeterRow(
                        label = "CPU Load",
                        valueText = "${if (status == ServerStatus.RUNNING) metrics.cpuPercent else 0}%",
                        progress = if (status == ServerStatus.RUNNING) cpuProg else 0f,
                        barColor = if (cpuProg > 0.8f) PaperRed else PaperGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PLAYERS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text(
                                "${metrics.onlinePlayers}/${metrics.maxPlayers}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = PaperGreen
                            )
                        }
                        Column {
                            Text("CHUNKS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text(
                                "${metrics.chunksLoaded}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = PaperCyan
                            )
                        }
                        Column {
                            Text("NET I/O", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text(
                                "${metrics.networkTxKbps} KB/s",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    status: ServerStatus,
    onExecute: (String) -> Unit,
    onSimulateJoin: () -> Unit,
    onOpenFiles: () -> Unit = {},
    onOpenProperties: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "In-Game Quick Actions",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onExecute("time set day") },
                    enabled = status == ServerStatus.RUNNING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = "Day", modifier = Modifier.size(16.dp), tint = PaperGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Day", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onExecute("weather clear") },
                    enabled = status == ServerStatus.RUNNING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = PaperCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onExecute("kill @e[type=item]") },
                    enabled = status == ServerStatus.RUNNING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Lag", modifier = Modifier.size(16.dp), tint = PaperRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lag", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSimulateJoin,
                    enabled = status == ServerStatus.RUNNING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PaperSurfaceElevated,
                        contentColor = PaperGreen
                    )
                ) {
                    Text("+ Test Player Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = { onExecute("reload confirm") },
                    enabled = status == ServerStatus.RUNNING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PaperSurfaceElevated,
                        contentColor = PaperCyan
                    )
                ) {
                    Text("Reload Configs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenFiles,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PaperPurple.copy(alpha = 0.2f),
                        contentColor = PaperPurple
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Raw Files", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onOpenProperties,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PaperCyan.copy(alpha = 0.18f),
                        contentColor = PaperCyan
                    )
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(".properties", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun LiveConsoleSnippetCard(
    logs: List<com.example.server.ConsoleMessage>,
    onOpenConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Console",
                        tint = PaperCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Server Console",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                TextButton(onClick = onOpenConsole) {
                    Text("Open Terminal →", color = PaperCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PaperConsoleBg)
                    .border(1.dp, Color(0xFF1E262F), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Server offline. Start server to view live console logs.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        logs.forEach { log ->
                            MinecraftText(
                                text = "[${log.level}] ${log.text}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionGuideDialog(
    deviceIp: String,
    port: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("How to Connect to Server", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "1. Same Wi-Fi (LAN):",
                    fontWeight = FontWeight.Bold,
                    color = PaperCyan
                )
                Text(
                    "• PC (Java Edition): Multiplayer → Add Server → Server Address: $deviceIp:$port\n" +
                    "• Mobile / Console (Bedrock): Settings → Friends tab → Under \"LAN Games\", your server will appear automatically via Bedrock broadcast! Or Add Server with IP $deviceIp and Port $port."
                )

                Text(
                    "2. Playing with friends over the Internet:",
                    fontWeight = FontWeight.Bold,
                    color = PaperGreen
                )
                Text(
                    "To allow friends outside your home Wi-Fi to join, use Playit.gg (free Minecraft tunnel). Playit assigns a real .joinmc.link domain.\n\n" +
                    "⚠️ If Minecraft reports 'Unknown Host', it means the domain is not claimed in DNS yet. On home Wi-Fi/hotspot, always use Direct IP ($deviceIp:$port) which never gets Unknown Host!"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
            ) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PojavTestClientCard(
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperPurple.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onLaunch() }
            .testTag("pojav_test_client_card"),
        colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaperPurple.copy(alpha = 0.2f))
                        .border(1.5.dp, PaperPurple, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = "Pojav 1.21.1",
                        tint = PaperPurple,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pojav 1.21.1 Test Client",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PaperCyan.copy(alpha = 0.2f))
                                .border(0.5.dp, PaperCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("POJAV CORE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PaperCyan)
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Simulate Minecraft Java 1.21.1 with touch controls, F3 debug, 3D world, & test server join!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = PaperPurple, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("launch_pojav_quick_btn")
            ) {
                Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

