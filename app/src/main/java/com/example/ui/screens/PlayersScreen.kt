package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PlayerRecordEntity
import com.example.server.LivePlayer
import com.example.ui.MainViewModel
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGold
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import com.example.ui.theme.PaperSurfaceElevated

@Composable
fun PlayersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()
    val onlinePlayers by viewModel.onlinePlayers.collectAsStateWithLifecycle()
    val allPlayers by viewModel.currentPlayers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var playerActionTarget by remember { mutableStateOf<LivePlayer?>(null) }

    val whitelisted = remember(allPlayers) { allPlayers.filter { it.isWhitelisted } }
    val operators = remember(allPlayers) { allPlayers.filter { it.isOp } }
    val banned = remember(allPlayers) { allPlayers.filter { it.isBanned } }

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
                text = { Text("Online (${onlinePlayers.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Whitelist (${whitelisted.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("OPs (${operators.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Bans (${banned.size})", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Online players list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Connected Players (${onlinePlayers.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Button(
                                onClick = { viewModel.simulatePlayerJoin("Player_${(10..99).random()}") },
                                colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Simulate", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (onlinePlayers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Person, contentDescription = "No players", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No players currently connected.", color = Color.Gray)
                                    Text("Tap 'Test Join' or connect via Minecraft on LAN.", fontSize = 12.sp, color = PaperCyan)
                                }
                            }
                        }
                    } else {
                        items(onlinePlayers, key = { it.uuid }) { player ->
                            LivePlayerCard(
                                player = player,
                                onAction = { playerActionTarget = player }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Whitelist Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Whitelisted Players", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Button(
                                onClick = { showAddPlayerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add to whitelist", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Player", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (whitelisted.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                Text("No whitelisted players. Everyone can join if whitelist is disabled.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(whitelisted, key = { it.id }) { item ->
                            PlayerRecordRow(
                                record = item,
                                badgeText = "Whitelisted",
                                badgeColor = PaperCyan,
                                onDelete = { viewModel.deletePlayer(item) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Operators Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Server Operators (OP)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Button(
                                onClick = { showAddPlayerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PaperGold, contentColor = PaperDarkBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add OP", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add OP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (operators.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                Text("No operators assigned. Use /op <player> in Console or Add OP.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(operators, key = { it.id }) { item ->
                            PlayerRecordRow(
                                record = item,
                                badgeText = "Level ${item.opLevel} OP",
                                badgeColor = PaperGold,
                                onDelete = { viewModel.savePlayer(item.copy(isOp = false)) }
                            )
                        }
                    }
                }
            }

            3 -> {
                // Bans Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Banned Players", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PaperRed)
                    }

                    if (banned.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                Text("No players are currently banned.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(banned, key = { it.id }) { item ->
                            PlayerRecordRow(
                                record = item,
                                badgeText = "Banned: ${item.banReason.ifBlank { "Violation" }}",
                                badgeColor = PaperRed,
                                onDelete = { viewModel.savePlayer(item.copy(isBanned = false)) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Player action dialog
    playerActionTarget?.let { target ->
        PlayerActionDialog(
            player = target,
            onDismiss = { playerActionTarget = null },
            onKick = { reason ->
                viewModel.sendCommand("kick ${target.name} $reason")
                playerActionTarget = null
            },
            onBan = { reason ->
                viewModel.sendCommand("ban ${target.name} $reason")
                currentServer?.let { s ->
                    viewModel.savePlayer(
                        PlayerRecordEntity(
                            serverId = s.id,
                            username = target.name,
                            isBanned = true,
                            banReason = reason
                        )
                    )
                }
                playerActionTarget = null
            },
            onSetGamemode = { mode ->
                viewModel.sendCommand("gamemode $mode ${target.name}")
                playerActionTarget = null
            },
            onOp = {
                viewModel.sendCommand("op ${target.name}")
                currentServer?.let { s ->
                    viewModel.savePlayer(
                        PlayerRecordEntity(
                            serverId = s.id,
                            username = target.name,
                            isOp = true,
                            opLevel = 4
                        )
                    )
                }
                playerActionTarget = null
            }
        )
    }

    if (showAddPlayerDialog) {
        AddPlayerDialog(
            isOpMode = selectedTab == 2,
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { username ->
                currentServer?.let { s ->
                    val isOp = selectedTab == 2
                    val isWhitelisted = selectedTab == 1 || isOp
                    viewModel.savePlayer(
                        PlayerRecordEntity(
                            serverId = s.id,
                            username = username,
                            isOp = isOp,
                            isWhitelisted = isWhitelisted
                        )
                    )
                    if (isOp) viewModel.sendCommand("op $username")
                    if (isWhitelisted) viewModel.sendCommand("whitelist add $username")
                }
                showAddPlayerDialog = false
            }
        )
    }
}

@Composable
fun LivePlayerCard(
    player: LivePlayer,
    onAction: () -> Unit
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PaperCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = PaperCyan,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${player.gamemode} • Ping: ${player.pingMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperGreen
                        )
                    }
                }

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PaperSurfaceElevated, contentColor = PaperCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Actions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pos: (${player.x}, ${player.y}, ${player.z})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = player.dimension.replace("minecraft:", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperCyan,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PlayerRecordRow(
    record: PlayerRecordEntity,
    badgeText: String,
    badgeColor: Color,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(record.username, fontWeight = FontWeight.Bold, color = Color.White)
                Text(badgeText, fontSize = 11.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = PaperRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PlayerActionDialog(
    player: LivePlayer,
    onDismiss: () -> Unit,
    onKick: (reason: String) -> Unit,
    onBan: (reason: String) -> Unit,
    onSetGamemode: (mode: String) -> Unit,
    onOp: () -> Unit
) {
    var reason by remember { mutableStateOf("Kicked by server host") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage: ${player.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Kick / Ban") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Quick Actions:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onSetGamemode("creative") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Creative", fontSize = 11.sp) }

                    FilledTonalButton(
                        onClick = { onSetGamemode("survival") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Survival", fontSize = 11.sp) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOp,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = PaperGold.copy(alpha = 0.2f), contentColor = PaperGold)
                    ) { Text("Make OP", fontSize = 11.sp) }

                    Button(
                        onClick = { onKick(reason) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PaperRed, contentColor = Color.White)
                    ) { Text("Kick", fontSize = 11.sp) }
                }

                Button(
                    onClick = { onBan(reason) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaperRed, contentColor = Color.White)
                ) { Text("Ban Player", fontSize = 12.sp) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AddPlayerDialog(
    isOpMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (username: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isOpMode) "Add Operator" else "Add Whitelist Player", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Minecraft Username") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (username.isNotBlank()) onConfirm(username.trim()) },
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
