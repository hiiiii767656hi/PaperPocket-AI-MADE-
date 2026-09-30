package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.server.LogLevel
import com.example.ui.MainViewModel
import com.example.ui.components.MinecraftText
import com.example.ui.theme.PaperConsoleBg
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGold
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsoleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()

    var commandText by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var selectedLevelFilter by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    val filteredLogs by remember(logs, selectedLevelFilter, searchQuery) {
        derivedStateOf {
            logs.filter { entry ->
                val matchesFilter = selectedLevelFilter == null || entry.level == selectedLevelFilter
                val matchesSearch = searchQuery.isBlank() ||
                        entry.text.contains(searchQuery, ignoreCase = true) ||
                        entry.tag.contains(searchQuery, ignoreCase = true)
                matchesFilter && matchesSearch
            }
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    val quickCommands = listOf(
        "/tps",
        "/help",
        "/plugins",
        "/time set day",
        "/weather clear",
        "/kill @e[type=item]",
        "/say Hello World!",
        "/reload confirm",
        "/stop"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperDarkBg)
    ) {
        // Console Header Bar
        Surface(
            color = PaperSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE CONSOLE (${filteredLogs.size} lines)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        color = PaperCyan
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { autoScroll = !autoScroll },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (autoScroll) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (autoScroll) "Auto-scroll ON" else "Auto-scroll OFF",
                                tint = if (autoScroll) PaperGreen else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val allText = filteredLogs.joinToString("\n") { "[${it.level}] ${it.text}" }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Paper Logs", allText))
                                Toast.makeText(context, "Copied logs to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy logs",
                                tint = PaperCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearConsole() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear logs",
                                tint = PaperRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Filter & Search row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search bar mini
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        placeholder = { Text("Filter logs...", fontSize = 11.sp, color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PaperDarkBg,
                            unfocusedContainerColor = PaperDarkBg,
                            focusedBorderColor = PaperCyan,
                            unfocusedBorderColor = PaperSurfaceBorder,
                            cursorColor = PaperCyan
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Level Filter Chips
                    val levels = listOf(null, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(levels) { lvl ->
                            val label = lvl?.name ?: "ALL"
                            val isSelected = selectedLevelFilter == lvl
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLevelFilter = if (isSelected) null else lvl },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (lvl) {
                                        LogLevel.WARN -> PaperGold.copy(alpha = 0.3f)
                                        LogLevel.ERROR -> PaperRed.copy(alpha = 0.3f)
                                        else -> PaperCyan.copy(alpha = 0.3f)
                                    },
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Terminal Log Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PaperConsoleBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (logs.isEmpty()) "Terminal idle. Start the server to see real-time output." else "No log messages match current filter.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                    )
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { entry ->
                        val timeStr = remember(entry.timestamp) { timeFormat.format(Date(entry.timestamp)) }
                        val (levelColor, levelLabel) = when (entry.level) {
                            LogLevel.INFO -> Pair(PaperCyan, "INFO")
                            LogLevel.WARN -> Pair(PaperGold, "WARN")
                            LogLevel.ERROR -> Pair(PaperRed, "ERROR")
                            LogLevel.DEBUG -> Pair(Color.Gray, "DEBUG")
                            LogLevel.COMMAND -> Pair(PaperGreen, "CMD")
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "[$timeStr ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            )
                            Text(
                                text = levelLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = levelColor
                                )
                            )
                            Text(
                                text = "]: ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            )
                            MinecraftText(
                                text = entry.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                defaultColor = Color(0xFFE0E0E0),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Command Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickCommands) { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaperSurfaceBorder)
                        .clickable {
                            viewModel.sendCommand(cmd)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cmd,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = PaperCyan
                    )
                }
            }
        }

        // Bottom Command Input
        Surface(
            color = PaperSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Command history up/down buttons
                IconButton(
                    onClick = {
                        if (commandHistory.isNotEmpty()) {
                            historyIndex = (historyIndex + 1).coerceAtMost(commandHistory.size - 1)
                            commandText = commandHistory[historyIndex]
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "History Up", tint = Color.LightGray)
                }

                IconButton(
                    onClick = {
                        if (commandHistory.isNotEmpty() && historyIndex > 0) {
                            historyIndex--
                            commandText = commandHistory[historyIndex]
                        } else if (historyIndex == 0) {
                            historyIndex = -1
                            commandText = ""
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "History Down", tint = Color.LightGray)
                }

                OutlinedTextField(
                    value = commandText,
                    onValueChange = {
                        commandText = it
                        historyIndex = -1
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    placeholder = {
                        Text("Enter command (e.g. /tps, /op Steve)...", fontSize = 12.sp, color = Color.Gray)
                    },
                    leadingIcon = {
                        Text(">", fontWeight = FontWeight.Black, color = PaperGreen, fontFamily = FontFamily.Monospace)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commandText.isNotBlank()) {
                            viewModel.sendCommand(commandText)
                            commandText = ""
                            historyIndex = -1
                        }
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
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (commandText.isNotBlank()) {
                            viewModel.sendCommand(commandText)
                            commandText = ""
                            historyIndex = -1
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaperGreen)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send Command",
                        tint = PaperDarkBg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
