package com.example.utils

import java.util.Locale
import java.util.regex.Pattern

data class DetectedStartScript(
    val scriptFileName: String,
    val rawContent: String,
    val jarFileName: String?,
    val detectedMinecraftVersion: String?,
    val detectedPaperBuild: Int?,
    val allocatedRamMb: Int?,
    val minRamMb: Int?,
    val useAikarFlags: Boolean,
    val customJvmArgs: String,
    val port: Int?,
    val nogui: Boolean,
    val worldName: String?,
    val explanations: List<String>
)

object StartScriptParser {

    /**
     * Parses a Minecraft server start script (e.g. start.bat, run.bat, start.cmd, start.sh)
     * and extracts server launch parameters, JVM flags, RAM, jar name, and properties.
     */
    fun parse(fileName: String, content: String): DetectedStartScript {
        val explanations = mutableListOf<String>()

        // 1. Normalize line endings and combine line continuations (^ in Windows batch, \ in Bash)
        val cleanedLines = mutableListOf<String>()
        var pendingLine = StringBuilder()

        content.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("::") || trimmed.startsWith("REM", ignoreCase = true) || trimmed.startsWith("#")) {
                return@forEach
            }
            if (trimmed.endsWith("^") || trimmed.endsWith("\\")) {
                pendingLine.append(trimmed.dropLast(1).trim()).append(" ")
            } else {
                pendingLine.append(trimmed)
                cleanedLines.add(pendingLine.toString())
                pendingLine = StringBuilder()
            }
        }
        if (pendingLine.isNotEmpty()) {
            cleanedLines.add(pendingLine.toString())
        }

        // Full script text for scanning
        val fullCommandText = cleanedLines.joinToString(" ")

        // 2. Parse RAM (-Xmx and -Xms)
        var ramMb: Int? = null
        var minRamMb: Int? = null

        val xmxPattern = Pattern.compile("-Xmx(\\d+)([kKmMgGtT]?)", Pattern.CASE_INSENSITIVE)
        val xmxMatcher = xmxPattern.matcher(fullCommandText)
        if (xmxMatcher.find()) {
            val amount = xmxMatcher.group(1)?.toLongOrNull() ?: 1024L
            val unit = xmxMatcher.group(2)?.uppercase(Locale.ROOT) ?: "M"
            ramMb = convertToMb(amount, unit)
            explanations.add("Allocated RAM adapted from -Xmx: ${ramMb} MB")
        }

        val xmsPattern = Pattern.compile("-Xms(\\d+)([kKmMgGtT]?)", Pattern.CASE_INSENSITIVE)
        val xmsMatcher = xmsPattern.matcher(fullCommandText)
        if (xmsMatcher.find()) {
            val amount = xmsMatcher.group(1)?.toLongOrNull() ?: 1024L
            val unit = xmsMatcher.group(2)?.uppercase(Locale.ROOT) ?: "M"
            minRamMb = convertToMb(amount, unit)
            if (ramMb == null) {
                ramMb = minRamMb
                explanations.add("Allocated RAM adapted from -Xms: ${ramMb} MB")
            }
        }

        // 3. Parse Server Jar (-jar <filename>)
        var jarName: String? = null
        val jarPattern = Pattern.compile("-jar\\s+[\"']?([^\"'\\s]+\\.jar)[\"']?", Pattern.CASE_INSENSITIVE)
        val jarMatcher = jarPattern.matcher(fullCommandText)
        if (jarMatcher.find()) {
            val rawJar = jarMatcher.group(1) ?: ""
            // Extract filename only in case of path like "bin/paper.jar"
            jarName = rawJar.replace("\\", "/").substringAfterLast("/")
            explanations.add("Target server JAR detected: $jarName")
        }

        // Detect Minecraft version and Paper build from jar name if possible
        var mcVersion: String? = null
        var paperBuild: Int? = null

        jarName?.let { name ->
            val versionPattern = Pattern.compile("(?:paper|purpur|spigot|folia|pufferfish)[-_](\\d+\\.\\d+(?:\\.\\d+)?)(?:[-_](\\d+))?", Pattern.CASE_INSENSITIVE)
            val vMatcher = versionPattern.matcher(name)
            if (vMatcher.find()) {
                mcVersion = vMatcher.group(1)
                paperBuild = vMatcher.group(2)?.toIntOrNull()
                val buildStr = if (paperBuild != null) " (build #$paperBuild)" else ""
                explanations.add("Detected Minecraft version $mcVersion$buildStr from JAR filename")
            } else {
                // Check generic version in jar name e.g. "1.21.1.jar" or "server-1.20.4.jar"
                val genericPattern = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)")
                val gMatcher = genericPattern.matcher(name)
                if (gMatcher.find()) {
                    mcVersion = gMatcher.group(1)
                    explanations.add("Detected Minecraft version $mcVersion from JAR filename")
                }
            }
        }

        // 4. Detect Aikar's Flags
        val hasAikarFlags = fullCommandText.contains("UseG1GC", ignoreCase = true) ||
                fullCommandText.contains("aikar", ignoreCase = true) ||
                fullCommandText.contains("ParallelRefProcEnabled", ignoreCase = true) ||
                fullCommandText.contains("MaxGCPauseMillis", ignoreCase = true) ||
                fullCommandText.contains("AlwaysPreTouch", ignoreCase = true)

        if (hasAikarFlags) {
            explanations.add("Aikar's Garbage Collector optimization flags detected and enabled")
        }

        // 5. Detect Custom JVM Arguments (flags starting with -D or non-Aikar -XX)
        val customJvmArgsList = mutableListOf<String>()
        val dFlagPattern = Pattern.compile("(-D[a-zA-Z0-9._-]+(?:=[^\\s\"']+|\"[^\"]*\")?)")
        val dMatcher = dFlagPattern.matcher(fullCommandText)
        while (dMatcher.find()) {
            val flag = dMatcher.group(1)
            if (flag != null && !flag.contains("using.aikars.flags", ignoreCase = true) && !flag.contains("aikars.new.flags", ignoreCase = true)) {
                customJvmArgsList.add(flag)
            }
        }

        val customJvmArgs = if (customJvmArgsList.isNotEmpty()) {
            val combined = customJvmArgsList.joinToString(" ")
            explanations.add("Custom JVM system properties preserved: $combined")
            combined
        } else {
            "-Dpaper.playerconnection.keepalive=30"
        }

        // 6. Detect CLI port or options
        var port: Int? = null
        val portPattern = Pattern.compile("(?:--port|-p)\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
        val portMatcher = portPattern.matcher(fullCommandText)
        if (portMatcher.find()) {
            port = portMatcher.group(1)?.toIntOrNull()
            if (port != null) {
                explanations.add("Server port overridden in script: $port")
            }
        }

        val nogui = fullCommandText.contains("--nogui", ignoreCase = true) || fullCommandText.contains("nogui", ignoreCase = true)
        if (nogui) {
            explanations.add("Headless --nogui mode specified in start script")
        }

        var worldName: String? = null
        val worldPattern = Pattern.compile("--world\\s+([^\\s\"']+|\"[^\"]*\")", Pattern.CASE_INSENSITIVE)
        val worldMatcher = worldPattern.matcher(fullCommandText)
        if (worldMatcher.find()) {
            worldName = worldMatcher.group(1)?.replace("\"", "")
            explanations.add("Custom level/world specified: $worldName")
        }

        return DetectedStartScript(
            scriptFileName = fileName,
            rawContent = content,
            jarFileName = jarName,
            detectedMinecraftVersion = mcVersion,
            detectedPaperBuild = paperBuild,
            allocatedRamMb = ramMb,
            minRamMb = minRamMb,
            useAikarFlags = hasAikarFlags,
            customJvmArgs = customJvmArgs,
            port = port,
            nogui = nogui,
            worldName = worldName,
            explanations = explanations
        )
    }

    private fun convertToMb(amount: Long, unit: String): Int {
        return when (unit.uppercase(Locale.ROOT)) {
            "G" -> (amount * 1024).toInt()
            "T" -> (amount * 1024 * 1024).toInt()
            "K" -> (amount / 1024).toInt().coerceAtLeast(128)
            "M" -> amount.toInt()
            else -> {
                // If larger than 100000 assume bytes
                if (amount > 100_000_000) {
                    (amount / (1024 * 1024)).toInt()
                } else {
                    amount.toInt()
                }
            }
        }
    }
}
