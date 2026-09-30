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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
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
import com.example.utils.ImportServerOptions
import com.example.utils.ZipAnalysisResult

@Composable
fun ImportServerZipDialog(
    analysis: ZipAnalysisResult?,
    isAnalyzing: Boolean,
    isExtracting: Boolean,
    extractionProgress: Float,
    extractionStatus: String,
    errorMessage: String?,
    onConfirmImport: (ImportServerOptions) -> Unit,
    onDismiss: () -> Unit
) {
    if (isAnalyzing) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            containerColor = PaperSurface,
            title = {
                Text("Analyzing Server ZIP", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PaperCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Inspecting archive structure...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Detecting start.bat, server.properties, jar builds & plugins",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {}
        )
        return
    }

    if (isExtracting) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            containerColor = PaperSurface,
            title = {
                Text("Importing Server Folder", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { extractionProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = PaperGreen,
                        trackColor = PaperSurfaceBorder
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(extractionProgress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = PaperGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Please keep app open",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = extractionStatus,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.LightGray,
                        maxLines = 2
                    )
                }
            },
            confirmButton = {}
        )
        return
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PaperSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Close, contentDescription = "Error", tint = PaperRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Failed", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(errorMessage, color = Color.LightGray)
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PaperSurfaceBorder, contentColor = Color.White)
                ) {
                    Text("Close")
                }
            }
        )
        return
    }

    if (analysis == null) return

    // Pre-fill fields from detected start.bat and server.properties
    val detectedScript = analysis.detectedStartScript
    val detectedProps = analysis.detectedProperties

    var serverName by remember(analysis) { mutableStateOf(analysis.defaultServerName) }
    var ramMb by remember(analysis) {
        // Use detected RAM from start.bat, cap sensibly for mobile default (max 4096MB or as detected)
        val detected = detectedScript?.allocatedRamMb ?: 1024
        mutableStateOf(detected.toString())
    }
    var port by remember(analysis) {
        val detected = detectedScript?.port ?: detectedProps?.port ?: 25565
        mutableStateOf(detected.toString())
    }
    var version by remember(analysis) {
        mutableStateOf(detectedScript?.detectedMinecraftVersion ?: "1.21.1")
    }
    var paperBuild by remember(analysis) {
        mutableStateOf((detectedScript?.detectedPaperBuild ?: 120).toString())
    }
    var useAikarFlags by remember(analysis) {
        mutableStateOf(detectedScript?.useAikarFlags ?: true)
    }
    var customJvmArgs by remember(analysis) {
        mutableStateOf(detectedScript?.customJvmArgs ?: "-Dpaper.playerconnection.keepalive=30")
    }
    var motd by remember(analysis) {
        mutableStateOf(detectedProps?.motd ?: "§a${analysis.defaultServerName} §7[Imported]")
    }
    var gamemode by remember(analysis) {
        mutableStateOf(detectedProps?.gamemode ?: "survival")
    }
    var difficulty by remember(analysis) {
        mutableStateOf(detectedProps?.difficulty ?: "normal")
    }

    var showScriptDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
        title = {
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
                            .background(PaperCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = "ZIP Import", tint = PaperCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Import Server from ZIP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(
                            text = "${analysis.zipFileName} (${analysis.totalFiles} files • ${FileUtils.formatBytes(analysis.totalEstimatedSizeBytes)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // START.BAT DETECTION CARD
                if (detectedScript != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PaperGreen.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Terminal, contentDescription = "Script", tint = PaperGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = detectedScript.scriptFileName,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PaperGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("DETECTED & ADAPTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PaperGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Explanations checklist
                            detectedScript.explanations.forEach { exp ->
                                Row(
                                    modifier = Modifier.padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Adapted",
                                        tint = PaperGreen,
                                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = exp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCFD8DC)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (showScriptDetails) "Hide original script ▲" else "View original script ▼",
                                color = PaperCyan,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { showScriptDetails = !showScriptDetails }
                                    .padding(vertical = 2.dp)
                            )

                            if (showScriptDetails) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PaperDarkBg)
                                        .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = detectedScript.rawContent.trim(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = PaperCyan,
                                        maxLines = 10
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PaperGold.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = "Default Script", tint = PaperGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No start.bat found in ZIP root. Standard PaperMC startup will be generated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // ARCHIVE CONTENT SUMMARY (Jar, World, Plugins)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Server Jar
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PaperSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = PaperPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Server JAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaperPurple)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = analysis.serverJarFileName ?: "Standard Paper",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    // World & Plugins
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PaperSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = PaperCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${analysis.pluginsFound.size} Plugins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaperCyan)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (analysis.hasWorld) "World: ${analysis.worldName}" else "Default world",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                        }
                    }
                }

                // EDITABLE CONFIGURATION SECTION
                Text("Configuration & Launch Options", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaperCyan)

                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("import_server_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ramMb,
                        onValueChange = { ramMb = it },
                        label = { Text("RAM (MB)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("import_ram_input"),
                        trailingIcon = {
                            Text("MB", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                        }
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("import_port_input")
                    )
                }

                // Quick RAM preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick RAM:", fontSize = 11.sp, color = Color.Gray)
                    listOf("1024", "1536", "2048", "3072", "4096").forEach { mb ->
                        val isSelected = ramMb == mb
                        FilterChip(
                            selected = isSelected,
                            onClick = { ramMb = mb },
                            label = { Text("${mb}M", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaperCyan.copy(alpha = 0.25f),
                                selectedLabelColor = PaperCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = PaperSurfaceBorder,
                                selectedBorderColor = PaperCyan
                            )
                        )
                    }
                }

                // Aikar's flags toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaperSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PaperSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aikar's Garbage Collector Flags", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Optimized G1GC flags for smooth on-device TPS", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = useAikarFlags,
                            onCheckedChange = { useAikarFlags = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PaperGreen, checkedTrackColor = PaperGreen.copy(alpha = 0.4f))
                        )
                    }
                }

                // Custom JVM args input
                OutlinedTextField(
                    value = customJvmArgs,
                    onValueChange = { customJvmArgs = it },
                    label = { Text("JVM Arguments") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRam = ramMb.toIntOrNull() ?: 1024
                    val finalPort = port.toIntOrNull() ?: 25565
                    val finalBuild = paperBuild.toIntOrNull() ?: 120
                    val options = ImportServerOptions(
                        serverName = serverName.ifBlank { "Imported Server" },
                        allocatedRamMb = finalRam,
                        port = finalPort,
                        minecraftVersion = version,
                        paperBuild = finalBuild,
                        useAikarFlags = useAikarFlags,
                        customJvmArgs = customJvmArgs,
                        motd = motd,
                        gamemode = gamemode,
                        difficulty = difficulty
                    )
                    onConfirmImport(options)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                modifier = Modifier.testTag("confirm_import_server_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import & Switch Server", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_server_button")
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
