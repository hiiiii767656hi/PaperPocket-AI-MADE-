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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
    availableVersions: List<String>,
    onSelectServer: (Long) -> Unit,
    onCreateServer: (name: String, version: String, port: Int, motd: String, gamemode: String, difficulty: String, ramMb: Int) -> Unit,
    onImportZip: () -> Unit = {},
    onDeleteServer: () -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("New Paper Server") }
    var newVersion by remember { mutableStateOf(availableVersions.firstOrNull() ?: "1.21.1") }
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

                    var versionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = versionExpanded,
                        onExpandedChange = { versionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Paper $newVersion",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("PaperMC Version") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = versionExpanded,
                            onDismissRequest = { versionExpanded = false }
                        ) {
                            availableVersions.take(10).forEach { ver ->
                                DropdownMenuItem(
                                    text = { Text("Paper $ver") },
                                    onClick = {
                                        newVersion = ver
                                        versionExpanded = false
                                    }
                                )
                            }
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PaperGreen else PaperSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectServer(s.id)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) PaperSurfaceElevated else PaperSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Dns,
                                        contentDescription = "Server",
                                        tint = if (isSelected) PaperGreen else PaperCyan
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(s.name, fontWeight = FontWeight.Bold, color = Color.White)
                                            if (s.importedFromZip) {
                                                Spacer(modifier = Modifier.width(6.dp))
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

                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = PaperGreen)
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
}
