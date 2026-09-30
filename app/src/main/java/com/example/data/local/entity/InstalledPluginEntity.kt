package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_plugins")
data class InstalledPluginEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long,
    val pluginId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean = true,
    val fileName: String,
    val fileSizeBytes: Long = 0,
    val source: String = "store", // "store", "modrinth", "file"
    val iconUrl: String? = null,
    val downloadUrl: String? = null,
    val installedAt: Long = System.currentTimeMillis()
)
