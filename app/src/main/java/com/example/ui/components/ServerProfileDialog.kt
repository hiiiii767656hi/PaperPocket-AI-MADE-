package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.data.local.entity.ServerProfileEntity
import com.example.ui.screens.outlinedColors
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperPurple
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import com.example.ui.theme.PaperSurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerProfileDialog(
    servers: List<ServerProfileEntity>,
    activeServer: ServerProfileEntity?,
    runningServerId: Long? = null,
    availableVersions: List<String>,
    onSelectServer: (Long) -> Unit,
    onStartServer: (Long) -> Unit = {},
    onOpenFiles: (Long) -> Unit = {},
    onCreateServer: (name: String, version: String, port: Int, motd: String, gamemode: String, difficulty: String, ramMb: Int) -> Unit,
    onImportZip: () -> Unit = {},
    onDeleteServer: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var serverToDelete by remember { mutableStateOf<ServerProfileEntity?>(null) }

    var newName by remember { mutableStateOf("New Paper Server") }
    var newVersion by remember { mutableStateOf(availableVersions.firstOrNull() ?: "26.3") }
    var newPort by remember { mutableStateOf("25565") }
    var newRam by remember { mutableStateOf("1024") }
    var newGamemode by remember { mutableStateOf("survival") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (showCreateForm) "Create Paper Server" else "Switch Server Profile", fontWeight = FontWeight.Bold)
        },
        text = {
            if (showCreateForm) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newVersion,
                        onValueChange = { newVersion = it },
                        label = { Text("Minecraft Version (e.g. 26.3, 1.21.4, 1.20.4)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_server_version_input")
                    )

                    val createVersionChips = remember(availableVersions) {
                        (listOf("26.3", "1.21.4", "1.21.1", "1.20.4", "1.20.1", "1.19.4", "1.16.5", "1.12.2", "1.8.8") + availableVersions).distinct().take(10)
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(createVersionChips) { ver ->
                            FilterChip(
                                selected = newVersion.equals(ver, ignoreCase = true),
                                onClick = { newVersion = ver },
                                label = { Text(ver, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PaperCyan,
                                    selectedLabelColor = PaperDarkBg
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPort,
                            onValueChange = { newPort = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newRam,
                            onValueChange = { newRam = it },
                            label = { Text("RAM (MB)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, PaperPurple.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
                                    onImportZip()
                                }
                                .testTag("dialog_import_zip_button"),
                            colors = CardDefaults.cardColors(containerColor = PaperPurple.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                    Text("Import Server (.zip)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("Auto-adapts to start.bat, RAM & plugins", fontSize = 11.sp, color = PaperPurple)
                                }
                                Icon(Icons.Default.Add, contentDescription = "Import", tint = PaperPurple, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    items(servers, key = { it.id }) { s ->
                        val isSelected = s.id == activeServer?.id
                        val isRunning = s.id == runningServerId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isRunning) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isRunning) PaperGreen else if (isSelected) PaperCyan else PaperSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectServer(s.id)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isRunning || isSelected) PaperSurfaceElevated else PaperSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Dns,
                                            contentDescription = "Server",
                                            tint = if (isRunning) PaperGreen else if (isSelected) PaperCyan else Color.LightGray
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(s.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                if (isRunning) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(PaperGreen.copy(alpha = 0.25f))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("ONLINE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PaperGreen)
                                                    }
                                                } else if (isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(PaperCyan.copy(alpha = 0.25f))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PaperCyan)
                                                    }
                                                }
                                                if (s.importedFromZip) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(PaperPurple.copy(alpha = 0.25f))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("ZIP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PaperPurple)
                                                    }
                                                }
                                            }
                                            Text("Paper ${s.minecraftVersion} • Port ${s.port} • ${s.allocatedRamMb}MB RAM", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Quick Start Server Button
                                        if (!isRunning) {
                                            IconButton(
                                                onClick = {
                                                    onStartServer(s.id)
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("start_server_btn_${s.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Start Server",
                                                    tint = PaperGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Quick Files Button
                                        IconButton(
                                            onClick = {
                                                onOpenFiles(s.id)
                                                onDismiss()
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("open_files_btn_${s.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.FolderOpen,
                                                contentDescription = "Server Files",
                                                tint = PaperPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete Server Button (Always available)
                                        IconButton(
                                            onClick = { serverToDelete = s },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("delete_server_profile_${s.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Server",
                                                tint = PaperRed.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateForm) {
                Button(
                    onClick = {
                        onCreateServer(
                            newName,
                            newVersion,
                            newPort.toIntOrNull() ?: 25565,
                            "§a$newName §7[Paper $newVersion]",
                            newGamemode,
                            "normal",
                            newRam.toIntOrNull() ?: 1024
                        )
                        showCreateForm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg)
                ) {
                    Text("Create Profile", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { showCreateForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Profile", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Profile", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showCreateForm) showCreateForm = false else onDismiss()
                }
            ) {
                Text(if (showCreateForm) "Back" else "Close")
            }
        }
    )

    if (serverToDelete != null) {
        val target = serverToDelete!!
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            containerColor = PaperSurface,
            title = {
                Text("Delete '${target.name}'?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${target.name}'? This will permanently delete its configuration, world saves, plugins, and directory files.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        serverToDelete = null
                        onDeleteServer(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperRed, contentColor = Color.White)
                ) {
                    Text("Delete Server", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
