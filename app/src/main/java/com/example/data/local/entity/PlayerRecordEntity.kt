package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_records")
data class PlayerRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long,
    val username: String,
    val uuid: String = "",
    val isOp: Boolean = false,
    val opLevel: Int = 4,
    val isWhitelisted: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String = "",
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)
