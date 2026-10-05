package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.utils.FileManagerHelper
import com.example.utils.ServerFileItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()

    BackHandler {
        onNavigateBack()
    }

    val rootDir = remember(currentServer) {
        currentServer?.getEffectiveDirectory(context) ?: File(context.filesDir, "servers/1")
    }

    var currentDir by remember(rootDir) { mutableStateOf(rootDir) }
    var fileList by remember { mutableStateOf<List<ServerFileItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dialogs
    var showChangeDirDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ServerFileItem?>(null) }
    var itemToRename by remember { mutableStateOf<ServerFileItem?>(null) }

    // Editor state
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editingFileContent by remember { mutableStateOf("") }
    var isSavingFile by remember { mutableStateOf(false) }

    fun refreshFiles() {
        if (!currentDir.exists()) currentDir.mkdirs()
        fileList = FileManagerHelper.listDirectory(currentDir)
    }

    LaunchedEffect(currentDir, currentServer) {
        refreshFiles()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PaperDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Raw File Manager",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PaperGold.copy(alpha = 0.2f))
                                    .border(1.dp, PaperGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("FILES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PaperGold)
                            }
                        }
                        Text(
                            text = currentServer?.name ?: "Server Files",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("file_manager_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Change Directory Button
                    Button(
                        onClick = { showChangeDirDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaperPurple.copy(alpha = 0.25f),
                            contentColor = PaperPurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("open_change_dir_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Change Directory", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Dir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchActive) PaperCyan else Color.LightGray)
                    }

                    IconButton(onClick = { refreshFiles() }, modifier = Modifier.testTag("refresh_files_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Working Directory Path Banner
            Surface(
                color = PaperSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = PaperGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Working Directory:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        val isCustom = !currentServer?.customServerDirectory.isNullOrBlank()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCustom) PaperPurple.copy(alpha = 0.25f) else PaperGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isCustom) "CUSTOM DIR" else "APP INTERNAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustom) PaperPurple else PaperGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = rootDir.absolutePath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = PaperCyan,
                        maxLines = 1
                    )
                }
            }

            // Breadcrumbs Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PaperSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigate Up button if inside subdirectory
                if (currentDir != rootDir && currentDir.parentFile != null) {
                    IconButton(
                        onClick = {
                            currentDir = currentDir.parentFile ?: rootDir
                        },
                        modifier = Modifier.size(32.dp).testTag("folder_up_button")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = PaperCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Breadcrumb path chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentDir == rootDir,
                        onClick = { currentDir = rootDir },
                        label = { Text("root", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaperCyan.copy(alpha = 0.25f),
                            selectedLabelColor = PaperCyan
                        )
                    )

                    val relativePath = currentDir.absolutePath.removePrefix(rootDir.absolutePath).trimStart('/')
                    if (relativePath.isNotEmpty()) {
                        val segments = relativePath.split('/')
                        var accumulated = rootDir.absolutePath
                        segments.forEachIndexed { index, seg ->
                            accumulated += "/$seg"
                            val targetPath = accumulated
                            Text(" / ", color = Color.Gray, fontSize = 12.sp)
                            FilterChip(
                                selected = index == segments.lastIndex,
                                onClick = { currentDir = File(targetPath) },
                                label = { Text(seg, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PaperCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = PaperCyan
                                )
                            )
                        }
                    }
                }

                // New File & Folder actions
                IconButton(onClick = { showNewFileDialog = true }, modifier = Modifier.size(32.dp).testTag("new_file_button")) {
                    Icon(Icons.Default.Add, contentDescription = "New File", tint = PaperGreen, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.size(32.dp).testTag("new_folder_button")) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = PaperGold, modifier = Modifier.size(20.dp))
                }
            }

            // Search Bar
            AnimatedVisibility(visible = isSearchActive) {
                Surface(color = PaperSurfaceElevated) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("file_search_input"),
                        placeholder = { Text("Search files & folders...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PaperCyan) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = outlinedColors()
                    )
                }
            }

            // File List
            val displayedFiles = remember(fileList, searchQuery) {
                if (searchQuery.isBlank()) fileList
                else fileList.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "Folder is empty",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create a file or folder in this directory",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayedFiles, key = { it.path }) { item ->
                        FileItemCard(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    currentDir = File(item.path)
                                } else if (item.isEditableText) {
                                    val f = File(item.path)
                                    editingFile = f
                                    editingFileContent = FileManagerHelper.readFileText(f)
                                } else {
                                    Toast.makeText(context, "${item.name} (${item.formattedSize})", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEdit = {
                                val f = File(item.path)
                                editingFile = f
                                editingFileContent = FileManagerHelper.readFileText(f)
                            },
                            onRename = { itemToRename = item },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Change Directory Dialog
    if (showChangeDirDialog) {
        var dirInput by remember { mutableStateOf(rootDir.absolutePath) }
        val defaultPath = remember(currentServer) {
            File(context.filesDir, "servers/${currentServer?.id ?: 1}").absolutePath
        }
        val documentsPath = remember(currentServer) {
            "/storage/emulated/0/Documents/PaperServer_${currentServer?.name?.replace(" ", "_") ?: "1"}"
        }

        AlertDialog(
            onDismissRequest = { showChangeDirDialog = false },
            containerColor = PaperSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PaperPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Server Directory", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set the folder where this server's worlds, plugins, and configuration files live on your device.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = dirInput,
                        onValueChange = { dirInput = it },
                        label = { Text("Server Directory Path") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("custom_dir_input"),
                        colors = outlinedColors()
                    )

                    Text("Quick Location Presets:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = dirInput == defaultPath,
                            onClick = { dirInput = defaultPath },
                            label = { Text("App Internal", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = dirInput == documentsPath,
                            onClick = { dirInput = documentsPath },
                            label = { Text("Documents Folder", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = dirInput.trim()
                        if (path.isNotBlank()) {
                            viewModel.updateServerDirectory(path)
                            val newDir = File(path)
                            if (!newDir.exists()) newDir.mkdirs()
                            currentDir = newDir
                            showChangeDirDialog = false
                            Toast.makeText(context, "Server directory updated to: $path", Toast.LENGTH_LONG).show()
                            refreshFiles()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperPurple, contentColor = Color.White),
                    modifier = Modifier.testTag("apply_change_dir_button")
                ) {
                    Text("Apply & Set Directory", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeDirDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = PaperSurface,
            title = { Text("Create New File", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter filename (e.g. motd.txt, config.yml, ops.json):", fontSize = 12.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it.trim() },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val success = FileManagerHelper.createNewFile(currentDir, newFileName)
                            if (success) {
                                refreshFiles()
                                showNewFileDialog = false
                                Toast.makeText(context, "Created $newFileName", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to create file (already exists?)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg)
                ) {
                    Text("Create File", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = PaperSurface,
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter folder name (e.g. plugins, worlds, scripts):", fontSize = 12.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it.trim() },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val success = FileManagerHelper.createNewFolder(currentDir, newFolderName)
                            if (success) {
                                refreshFiles()
                                showNewFolderDialog = false
                                Toast.makeText(context, "Created folder $newFolderName", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGold, contentColor = PaperDarkBg)
                ) {
                    Text("Create Folder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Rename Item Dialog
    if (itemToRename != null) {
        val target = itemToRename!!
        var renameInput by remember(target) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            containerColor = PaperSurface,
            title = { Text("Rename ${if (target.isDirectory) "Folder" else "File"}", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it.trim() },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank() && renameInput != target.name) {
                            val success = FileManagerHelper.renameItem(File(target.path), renameInput)
                            if (success) {
                                refreshFiles()
                                itemToRename = null
                                Toast.makeText(context, "Renamed to $renameInput", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
                ) {
                    Text("Rename", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Delete Item Dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = PaperSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = PaperRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete ${if (target.isDirectory) "Folder" else "File"}?", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete '${target.name}'? This action cannot be undone.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FileManagerHelper.deleteItem(File(target.path))
                        refreshFiles()
                        itemToDelete = null
                        Toast.makeText(context, "Deleted ${target.name}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperRed, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Built-in Raw Code / Text File Editor
    if (editingFile != null) {
        val f = editingFile!!
        AlertDialog(
            onDismissRequest = { editingFile = null },
            containerColor = PaperSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(f.name, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, maxLines = 1)
                        Text("${f.length()} bytes • ${editingFileContent.lines().size} lines", style = MaterialTheme.typography.labelSmall, color = PaperCyan)
                    }
                    IconButton(onClick = { editingFile = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    OutlinedTextField(
                        value = editingFileContent,
                        onValueChange = { editingFileContent = it },
                        modifier = Modifier.fillMaxSize().testTag("raw_file_editor_textarea"),
                        colors = outlinedColors(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFECEFF1)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingFile = true
                        val success = FileManagerHelper.writeFileText(f, editingFileContent)
                        isSavingFile = false
                        if (success) {
                            Toast.makeText(context, "Saved ${f.name}", Toast.LENGTH_SHORT).show()
                            refreshFiles()
                            editingFile = null
                        } else {
                            Toast.makeText(context, "Failed to save ${f.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                    modifier = Modifier.testTag("save_file_button")
                ) {
                    if (isSavingFile) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PaperDarkBg, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save File", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFile = null }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun FileItemCard(
    item: ServerFileItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("file_item_${item.name}"),
        colors = CardDefaults.cardColors(containerColor = PaperSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                item.isDirectory -> PaperGold.copy(alpha = 0.2f)
                                item.extension == "jar" -> PaperPurple.copy(alpha = 0.25f)
                                item.extension in listOf("properties", "yml", "yaml") -> PaperCyan.copy(alpha = 0.2f)
                                item.extension == "json" -> PaperGreen.copy(alpha = 0.2f)
                                item.extension in listOf("bat", "sh", "cmd") -> PaperRed.copy(alpha = 0.2f)
                                else -> PaperSurfaceElevated
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            item.isDirectory -> Icons.Default.Folder
                            item.extension == "jar" -> Icons.Default.Extension
                            item.extension in listOf("properties", "yml", "yaml") -> Icons.Default.Tune
                            item.extension == "json" -> Icons.Default.Code
                            item.extension in listOf("bat", "sh", "cmd") -> Icons.Default.Terminal
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = when {
                            item.isDirectory -> PaperGold
                            item.extension == "jar" -> PaperPurple
                            item.extension in listOf("properties", "yml", "yaml") -> PaperCyan
                            item.extension == "json" -> PaperGreen
                            item.extension in listOf("bat", "sh", "cmd") -> PaperRed
                            else -> Color.LightGray
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.name,
                        fontWeight = if (item.isDirectory) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = if (!item.isDirectory) FontFamily.Monospace else FontFamily.Default,
                        maxLines = 1
                    )
                    Text(
                        text = "${item.formattedSize} • ${item.formattedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isEditableText) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PaperCyan, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Rename", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PaperRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
