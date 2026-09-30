package com.example.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class SelectedFileInfo(
    val fileName: String,
    val sizeBytes: Long,
    val localFilePath: String? = null
)

object FileUtils {

    fun getFileInfo(context: Context, uri: Uri): SelectedFileInfo {
        var name = "unknown.jar"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: name
                }
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return SelectedFileInfo(name, size)
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri, subDir: String, customFileName: String? = null): SelectedFileInfo {
        val info = getFileInfo(context, uri)
        val finalName = customFileName ?: info.fileName
        val targetDir = File(context.filesDir, subDir)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val targetFile = File(targetDir, finalName)

        context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
            FileOutputStream(targetFile).use { output: FileOutputStream ->
                input.copyTo(output)
            }
        }

        return SelectedFileInfo(
            fileName = finalName,
            sizeBytes = targetFile.length(),
            localFilePath = targetFile.absolutePath
        )
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
