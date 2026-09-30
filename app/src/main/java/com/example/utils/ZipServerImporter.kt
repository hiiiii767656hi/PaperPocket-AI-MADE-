package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entity.InstalledPluginEntity
import com.example.data.local.entity.PlayerRecordEntity
import com.example.data.local.entity.ServerProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ImportedPluginInfo(
    val fileName: String,
    val name: String,
    val version: String,
    val sizeBytes: Long
)

data class ZipAnalysisResult(
    val zipFileName: String,
    val defaultServerName: String,
    val totalFiles: Int,
    val totalEstimatedSizeBytes: Long,
    val detectedStartScript: DetectedStartScript?,
    val detectedProperties: DetectedServerProperties?,
    val serverJarFileName: String?,
    val serverJarSizeBytes: Long,
    val pluginsFound: List<ImportedPluginInfo>,
    val hasWorld: Boolean,
    val worldName: String,
    val hasOps: Boolean,
    val hasWhitelist: Boolean,
    val commonPrefixToStrip: String
)

data class ImportServerOptions(
    val serverName: String,
    val allocatedRamMb: Int,
    val port: Int,
    val minecraftVersion: String,
    val paperBuild: Int,
    val useAikarFlags: Boolean,
    val customJvmArgs: String,
    val motd: String,
    val gamemode: String,
    val difficulty: String
)

object ZipServerImporter {

    /**
     * Inspects a ZIP archive from a content Uri, analyzing start.bat, server.properties,
     * plugins, jar files, and worlds without extracting the full archive to disk yet.
     */
    suspend fun analyzeZip(context: Context, uri: Uri): ZipAnalysisResult = withContext(Dispatchers.IO) {
        val fileInfo = FileUtils.getFileInfo(context, uri)
        val zipName = fileInfo.fileName
        val baseName = zipName.removeSuffix(".zip").removeSuffix(".ZIP")
            .replace("-", " ")
            .replace("_", " ")
            .trim()

        var totalFiles = 0
        var totalEstimatedSize = 0L

        var startScriptEntryName: String? = null
        var startScriptContent: String? = null

        var serverPropertiesContent: String? = null

        val detectedJarsAtRoot = mutableListOf<Pair<String, Long>>()
        val pluginsFound = mutableListOf<ImportedPluginInfo>()
        var hasWorld = false
        var worldName = "world"
        var hasOps = false
        var hasWhitelist = false

        val entryPaths = mutableListOf<String>()

        context.contentResolver.openInputStream(uri)?.use { rawInput ->
            ZipInputStream(BufferedInputStream(rawInput)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.replace("\\", "/")
                    if (!entry.isDirectory) {
                        totalFiles++
                        totalEstimatedSize += if (entry.size > 0) entry.size else 0L
                        entryPaths.add(name)

                        val lower = name.lowercase(Locale.ROOT)
                        val fileNameOnly = name.substringAfterLast("/")

                        // Check for start scripts (priority: start.bat, run.bat, start.cmd, launch.bat, start.sh, run.sh)
                        if (isStartScript(fileNameOnly)) {
                            if (startScriptContent == null || fileNameOnly.equals("start.bat", ignoreCase = true)) {
                                startScriptEntryName = fileNameOnly
                                startScriptContent = readEntryContent(zis)
                            }
                        }

                        // Check for server.properties
                        if (fileNameOnly.equals("server.properties", ignoreCase = true)) {
                            serverPropertiesContent = readEntryContent(zis)
                        }

                        // Check for ops.json / whitelist.json
                        if (fileNameOnly.equals("ops.json", ignoreCase = true)) {
                            hasOps = true
                        }
                        if (fileNameOnly.equals("whitelist.json", ignoreCase = true)) {
                            hasWhitelist = true
                        }

                        // Check for world indicator
                        if (lower.contains("level.dat")) {
                            hasWorld = true
                            val dir = name.substringBeforeLast("/")
                            val detectedWName = dir.substringAfterLast("/")
                            if (detectedWName.isNotBlank() && !detectedWName.equals("level", ignoreCase = true)) {
                                worldName = detectedWName
                            }
                        }

                        // Check for plugins
                        if (lower.contains("/plugins/") || lower.startsWith("plugins/")) {
                            if (fileNameOnly.endsWith(".jar", ignoreCase = true)) {
                                val cleanPlugin = parsePluginFileName(fileNameOnly, entry.size)
                                pluginsFound.add(cleanPlugin)
                            }
                        } else if (fileNameOnly.endsWith(".jar", ignoreCase = true)) {
                            // Root or bin .jar file
                            detectedJarsAtRoot.add(fileNameOnly to entry.size)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        // Determine if all entries share a single root directory (e.g. "MyServerFolder/...")
        val commonPrefix = detectCommonRootPrefix(entryPaths)

        // Parse detected start script if found
        val detectedScript = if (startScriptContent != null && startScriptEntryName != null) {
            StartScriptParser.parse(startScriptEntryName!!, startScriptContent!!)
        } else {
            null
        }

        // Parse server properties if found
        val detectedProperties = if (serverPropertiesContent != null) {
            ServerPropertiesParser.parse(serverPropertiesContent!!)
        } else {
            null
        }

        // Match server jar: either from start.bat or first root .jar
        var chosenJarName: String? = detectedScript?.jarFileName
        var chosenJarSize = 0L

        if (chosenJarName != null) {
            val matching = detectedJarsAtRoot.firstOrNull { it.first.equals(chosenJarName, ignoreCase = true) }
            if (matching != null) {
                chosenJarName = matching.first
                chosenJarSize = matching.second
            }
        } else {
            // Find likely server jar like paper*.jar, purpur*.jar, server.jar
            val likely = detectedJarsAtRoot.firstOrNull {
                val n = it.first.lowercase(Locale.ROOT)
                n.contains("paper") || n.contains("purpur") || n.contains("spigot") || n.contains("server")
            } ?: detectedJarsAtRoot.firstOrNull()

            if (likely != null) {
                chosenJarName = likely.first
                chosenJarSize = likely.second
            }
        }

        ZipAnalysisResult(
            zipFileName = zipName,
            defaultServerName = baseName.ifBlank { "Imported Server" },
            totalFiles = totalFiles,
            totalEstimatedSizeBytes = totalEstimatedSize,
            detectedStartScript = detectedScript,
            detectedProperties = detectedProperties,
            serverJarFileName = chosenJarName,
            serverJarSizeBytes = chosenJarSize,
            pluginsFound = pluginsFound,
            hasWorld = hasWorld,
            worldName = detectedProperties?.levelName ?: worldName,
            hasOps = hasOps,
            hasWhitelist = hasWhitelist,
            commonPrefixToStrip = commonPrefix
        )
    }

    /**
     * Extracts all files from the ZIP archive into internal storage:
     * context.filesDir/servers/<serverId>/
     * and persists the new ServerProfileEntity, InstalledPluginEntity records,
     * and player records to Room.
     */
    suspend fun extractAndCreateServer(
        context: Context,
        uri: Uri,
        analysis: ZipAnalysisResult,
        options: ImportServerOptions,
        database: AppDatabase,
        onProgress: (Float, String) -> Unit
    ): ServerProfileEntity = withContext(Dispatchers.IO) {
        val serverDao = database.serverDao()
        val pluginDao = database.pluginDao()
        val playerDao = database.playerDao()

        // 1. Pre-insert the server profile to get a permanent serverId
        val initialProfile = ServerProfileEntity(
            name = options.serverName,
            minecraftVersion = options.minecraftVersion,
            paperBuild = options.paperBuild,
            port = options.port,
            motd = options.motd,
            gamemode = options.gamemode,
            difficulty = options.difficulty,
            pvp = analysis.detectedProperties?.pvp ?: true,
            onlineMode = analysis.detectedProperties?.onlineMode ?: false,
            allocatedRamMb = options.allocatedRamMb,
            useAikarFlags = options.useAikarFlags,
            customJvmArgs = options.customJvmArgs,
            viewDistance = analysis.detectedProperties?.viewDistance ?: 10,
            simulationDistance = analysis.detectedProperties?.simulationDistance ?: 8,
            whiteListEnabled = analysis.detectedProperties?.whiteListEnabled ?: false,
            hardcore = analysis.detectedProperties?.hardcore ?: false,
            allowFlight = analysis.detectedProperties?.allowFlight ?: false,
            spawnProtection = analysis.detectedProperties?.spawnProtection ?: 16,
            levelName = analysis.worldName,
            seed = analysis.detectedProperties?.seed ?: "",
            customJarFileName = analysis.serverJarFileName,
            useCustomJar = analysis.serverJarFileName != null,
            customJarSizeBytes = analysis.serverJarSizeBytes,
            importedFromZip = true,
            originalStartScript = analysis.detectedStartScript?.rawContent,
            createdAt = System.currentTimeMillis()
        )

        val serverId = serverDao.insertServer(initialProfile)

        // 2. Prepare target directory
        val targetDir = File(context.filesDir, "servers/$serverId")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val prefixToStrip = analysis.commonPrefixToStrip
        var extractedCount = 0
        val totalToExtract = analysis.totalFiles.coerceAtLeast(1)

        var finalJarFile: File? = null
        var opsContent: String? = null
        var whitelistContent: String? = null

        // 3. Extract all entries
        context.contentResolver.openInputStream(uri)?.use { rawInput ->
            ZipInputStream(BufferedInputStream(rawInput)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(16 * 1024)

                while (entry != null) {
                    val rawName = entry.name.replace("\\", "/")
                    val cleanRelativePath = if (prefixToStrip.isNotEmpty() && rawName.startsWith(prefixToStrip)) {
                        rawName.removePrefix(prefixToStrip).removePrefix("/")
                    } else {
                        rawName
                    }

                    if (cleanRelativePath.isNotBlank()) {
                        val destinationFile = File(targetDir, cleanRelativePath)

                        // Guard against zip slip
                        val destinationCanonical = destinationFile.canonicalPath
                        val targetCanonical = targetDir.canonicalPath
                        if (!destinationCanonical.startsWith(targetCanonical)) {
                            // Skip unsafe entry
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }

                        if (entry.isDirectory) {
                            destinationFile.mkdirs()
                        } else {
                            destinationFile.parentFile?.mkdirs()

                            FileOutputStream(destinationFile).use { fos ->
                                var bytesRead: Int
                                while (zis.read(buffer).also { bytesRead = it } != -1) {
                                    fos.write(buffer, 0, bytesRead)
                                }
                            }

                            val fileNameOnly = destinationFile.name
                            if (analysis.serverJarFileName != null && fileNameOnly.equals(analysis.serverJarFileName, ignoreCase = true)) {
                                finalJarFile = destinationFile
                            }

                            if (fileNameOnly.equals("ops.json", ignoreCase = true)) {
                                opsContent = destinationFile.readText(StandardCharsets.UTF_8)
                            }
                            if (fileNameOnly.equals("whitelist.json", ignoreCase = true)) {
                                whitelistContent = destinationFile.readText(StandardCharsets.UTF_8)
                            }

                            extractedCount++
                            val progress = (extractedCount.toFloat() / totalToExtract).coerceIn(0f, 1f)
                            if (extractedCount % 5 == 0 || extractedCount == totalToExtract) {
                                onProgress(progress, "Extracting: $fileNameOnly ($extractedCount/$totalToExtract)")
                            }
                        }
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        // 4. Update server profile with verified local file path
        val updatedProfile = if (finalJarFile != null && finalJarFile!!.exists()) {
            initialProfile.copy(
                id = serverId,
                customJarFileName = finalJarFile!!.name,
                customJarUri = finalJarFile!!.absolutePath,
                customJarSizeBytes = finalJarFile!!.length(),
                useCustomJar = true
            )
        } else {
            initialProfile.copy(id = serverId)
        }
        serverDao.updateServer(updatedProfile)

        // 5. Register extracted plugins into Room
        val pluginsDir = File(targetDir, "plugins")
        if (pluginsDir.exists() && pluginsDir.isDirectory) {
            val jarFiles = pluginsDir.listFiles { f -> f.isFile && f.name.endsWith(".jar", ignoreCase = true) } ?: emptyArray()
            jarFiles.forEach { jarFile ->
                val p = parsePluginFileName(jarFile.name, jarFile.length())
                val entity = InstalledPluginEntity(
                    serverId = serverId,
                    pluginId = p.name.lowercase().replace(" ", "-"),
                    name = p.name,
                    version = p.version,
                    author = "Imported",
                    description = "Plugin imported from ZIP (${jarFile.name})",
                    category = "Imported",
                    isEnabled = true,
                    fileName = jarFile.name,
                    fileSizeBytes = jarFile.length(),
                    source = "zip",
                    installedAt = System.currentTimeMillis()
                )
                pluginDao.insertPlugin(entity)
            }
        }

        // 6. Register player records from ops.json & whitelist.json if available
        parsePlayersFromJson(opsContent, whitelistContent, serverId).forEach { player ->
            playerDao.insertOrUpdate(player)
        }

        onProgress(1f, "Import completed!")
        updatedProfile
    }

    private fun isStartScript(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower == "start.bat" || lower == "run.bat" || lower == "start.cmd" ||
                lower == "run.cmd" || lower == "launch.bat" || lower == "start.sh" ||
                lower == "run.sh"
    }

    private fun readEntryContent(zis: ZipInputStream): String {
        val reader = BufferedReader(InputStreamReader(zis, StandardCharsets.UTF_8))
        val sb = StringBuilder()
        var line: String? = reader.readLine()
        var count = 0
        while (line != null && count < 300) { // Limit script read to 300 lines
            sb.append(line).append("\n")
            count++
            line = reader.readLine()
        }
        return sb.toString()
    }

    private fun detectCommonRootPrefix(paths: List<String>): String {
        if (paths.isEmpty()) return ""
        val firstSlash = paths.firstOrNull { it.contains("/") }?.substringBefore("/") ?: return ""
        if (firstSlash.isBlank()) return ""

        // Check if all non-empty paths start with this directory
        val allShare = paths.all { it.startsWith("$firstSlash/") || it == firstSlash }
        return if (allShare) firstSlash else ""
    }

    private fun parsePluginFileName(fileName: String, sizeBytes: Long): ImportedPluginInfo {
        val base = fileName.removeSuffix(".jar").removeSuffix(".JAR")
        // Pattern like "EssentialsX-2.20.1" or "LuckPerms-Bukkit-5.4.102"
        val regex = Regex("^(.*?)[-_]v?(\\d+\\.\\d+.*)$")
        val match = regex.find(base)
        val name = match?.groupValues?.get(1)?.replace("-", " ")?.replace("_", " ") ?: base.replace("-", " ")
        val version = match?.groupValues?.get(2) ?: "1.0.0"

        return ImportedPluginInfo(
            fileName = fileName,
            name = name.ifBlank { fileName },
            version = version,
            sizeBytes = sizeBytes
        )
    }

    private fun parsePlayersFromJson(opsJson: String?, whitelistJson: String?, serverId: Long): List<PlayerRecordEntity> {
        val map = mutableMapOf<String, PlayerRecordEntity>()

        // Parse ops.json: [ { "uuid": "...", "name": "Steve", "level": 4 } ]
        if (!opsJson.isNullOrBlank()) {
            try {
                val array = JSONArray(opsJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        val uuid = obj.optString("uuid", "")
                        val level = obj.optInt("level", 4)
                        map[name.lowercase()] = PlayerRecordEntity(
                            serverId = serverId,
                            username = name,
                            uuid = uuid,
                            isOp = true,
                            opLevel = level,
                            isWhitelisted = false
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // Parse whitelist.json: [ { "uuid": "...", "name": "Alex" } ]
        if (!whitelistJson.isNullOrBlank()) {
            try {
                val array = JSONArray(whitelistJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        val uuid = obj.optString("uuid", "")
                        val existing = map[name.lowercase()]
                        if (existing != null) {
                            map[name.lowercase()] = existing.copy(isWhitelisted = true)
                        } else {
                            map[name.lowercase()] = PlayerRecordEntity(
                                serverId = serverId,
                                username = name,
                                uuid = uuid,
                                isOp = false,
                                isWhitelisted = true
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return map.values.toList()
    }
}
