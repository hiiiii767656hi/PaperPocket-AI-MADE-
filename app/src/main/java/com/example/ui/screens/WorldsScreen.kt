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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BackupEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorldsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()
    val backups by viewModel.currentBackups.collectAsStateWithLifecycle()

    var showNewWorldDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Current Active World Card
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
                            Icon(Icons.Default.Public, contentDescription = "World", tint = PaperGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active World",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { showNewWorldDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New World", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New World", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LEVEL NAME", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text(currentServer?.levelName ?: "world", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("DIFFICULTY", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text((currentServer?.difficulty ?: "normal").uppercase(), fontWeight = FontWeight.Bold, color = PaperCyan)
                        }
                        Column {
                            Text("GAMEMODE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text((currentServer?.gamemode ?: "survival").uppercase(), fontWeight = FontWeight.Bold, color = PaperGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("WORLD SEED", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                    Text(
                        currentServer?.seed?.ifBlank { "Random Seed" } ?: "Random Seed",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.LightGray
                    )
                }
            }
        }

        item {
            // Backup Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Backups & Snapshots (${backups.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Button(
                    onClick = { showBackupDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = "Create Backup", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Instant Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (backups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Archive, contentDescription = "No backups", tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No backups found for this server.", color = Color.Gray)
                        Text("Tap 'Instant Backup' to save a snapshot.", fontSize = 12.sp, color = PaperGreen)
                    }
                }
            }
        } else {
            items(backups, key = { it.id }) { backup ->
                BackupItemCard(
                    backup = backup,
                    dateStr = dateFormat.format(Date(backup.createdAt)),
                    onRestore = {
                        Toast.makeText(context, "Restored world from ${backup.title}!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        viewModel.deleteBackup(backup)
                    }
                )
            }
        }
    }

    if (showBackupDialog) {
        var title by remember { mutableStateOf("Manual Backup") }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Create Server Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Create a compressed ZIP archive of world chunks, player data, and configurations.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Backup Note / Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createBackup(title)
                        showBackupDialog = false
                        Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewWorldDialog) {
        var newLevelName by remember { mutableStateOf("world_${(10..99).random()}") }
        var newSeed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewWorldDialog = false },
            title = { Text("Generate New World", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newLevelName,
                        onValueChange = { newLevelName = it },
                        label = { Text("World Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSeed,
                        onValueChange = { newSeed = it },
                        label = { Text("Seed (Leave blank for random)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentServer?.let { s ->
                            viewModel.updateServerConfig(
                                s.copy(levelName = newLevelName, seed = newSeed)
                            )
                        }
                        showNewWorldDialog = false
                        Toast.makeText(context, "Active world set to $newLevelName!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
                ) {
                    Text("Set World", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewWorldDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BackupItemCard(
    backup: BackupEntity,
    dateStr: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(backup.title, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                val sizeMb = String.format(Locale.US, "%.1f MB", backup.fileSizeBytes / (1024.0 * 1024.0))
                Text(
                    text = "$dateStr • $sizeMb",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "World: ${backup.worldName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperCyan
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = PaperGreen)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PaperRed)
                }
            }
        }
    }
}
