package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
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
import com.example.server.ServerMetrics
import com.example.server.ServerStatus
import com.example.ui.MainViewModel
import com.example.ui.components.MinecraftText
import com.example.ui.components.ResourceMeterRow
import com.example.ui.components.TpsRadialGauge
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val server by viewModel.currentServer.collectAsStateWithLifecycle()
    val status by viewModel.serverStatus.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val deviceIp by viewModel.deviceIp.collectAsStateWithLifecycle()

    var showConnectionDialog by remember { mutableStateOf(false) }

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
                status = status,
                onStart = { viewModel.startServer() },
                onStop = { viewModel.stopServer() },
                onRestart = { viewModel.restartServer() }
            )
        }

        item {
            ConnectionCard(
                deviceIp = deviceIp,
                port = server?.port ?: 25565,
                status = status,
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
            PerformanceTelemetryCard(
                metrics = metrics,
                status = status
            )
        }

        item {
            QuickActionsCard(
                status = status,
                onExecute = { cmd -> viewModel.sendCommand(cmd) },
                onSimulateJoin = { viewModel.simulatePlayerJoin("Tester_${(10..99).random()}") }
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
}

@Composable
fun ServerHeaderCard(
    server: ServerProfileEntity?,
    status: ServerStatus,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
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

            Spacer(modifier = Modifier.height(12.dp))
            MinecraftText(
                text = server?.motd ?: "§7A PaperMC Server on Phone",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                                containerColor = PaperGreen,
                                contentColor = PaperDarkBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Server")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Paper Server", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    "To allow friends outside your home Wi-Fi to join, you can use Playit.gg or Ngrok (free Minecraft tunnels). In the app's Config tab, you can view the complete setup script!"
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
