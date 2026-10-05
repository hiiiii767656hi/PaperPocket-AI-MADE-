package com.example.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ServerFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val itemCount: Int = 0
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "$itemCount items"
            return FileUtils.formatBytes(sizeBytes)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val isEditableText: Boolean
        get() {
            if (isDirectory) return false
            val ext = extension.lowercase()
            return ext in listOf(
                "properties", "yml", "yaml", "json", "txt", "log",
                "bat", "sh", "cmd", "toml", "cfg", "conf", "md", "xml"
            ) || name in listOf("eula.txt", "server.properties", "ops.json", "whitelist.json", "banned-players.json")
        }
}

object FileManagerHelper {

    fun listDirectory(dir: File): List<ServerFileItem> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        return files.map { file ->
            val itemCount = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
            ServerFileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                extension = file.extension.lowercase(),
                itemCount = itemCount
            )
        }.sortedWith(compareByDescending<ServerFileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun readFileText(file: File): String {
        return if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else {
            ""
        }
    }

    fun writeFileText(file: File, content: String): Boolean {
        return try {
            if (file.parentFile?.exists() == false) {
                file.parentFile?.mkdirs()
            }
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFile(parentDir: File, fileName: String): Boolean {
        return try {
            val target = File(parentDir, fileName)
            if (!target.exists()) {
                target.createNewFile()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFolder(parentDir: File, folderName: String): Boolean {
        return try {
            val target = File(parentDir, folderName)
            if (!target.exists()) {
                target.mkdirs()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun deleteItem(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun renameItem(file: File, newName: String): Boolean {
        return try {
            val dest = File(file.parentFile, newName)
            file.renameTo(dest)
        } catch (_: Exception) {
            false
        }
    }
}
