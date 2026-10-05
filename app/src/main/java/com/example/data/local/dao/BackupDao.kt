package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM server_backups WHERE serverId = :serverId ORDER BY createdAt DESC")
    fun getBackupsForServer(serverId: Long): Flow<List<BackupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity): Long

    @Delete
    suspend fun deleteBackup(backup: BackupEntity)

    @Query("DELETE FROM server_backups WHERE id = :id")
    suspend fun deleteBackupById(id: Long)

    @Query("DELETE FROM server_backups WHERE serverId = :serverId")
    suspend fun deleteAllBackupsForServer(serverId: Long)
}
