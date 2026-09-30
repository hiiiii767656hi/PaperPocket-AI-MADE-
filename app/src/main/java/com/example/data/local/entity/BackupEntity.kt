package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_backups")
data class BackupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long,
    val title: String,
    val worldName: String,
    val fileSizeBytes: Long,
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis()
)
