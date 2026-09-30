package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ServerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM server_profiles ORDER BY id ASC")
    fun getAllServers(): Flow<List<ServerProfileEntity>>

    @Query("SELECT * FROM server_profiles WHERE id = :id LIMIT 1")
    fun getServerByIdFlow(id: Long): Flow<ServerProfileEntity?>

    @Query("SELECT * FROM server_profiles WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): ServerProfileEntity?

    @Query("SELECT * FROM server_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultServer(): ServerProfileEntity?

    @Query("SELECT COUNT(*) FROM server_profiles")
    suspend fun getServerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerProfileEntity): Long

    @Update
    suspend fun updateServer(server: ServerProfileEntity)

    @Delete
    suspend fun deleteServer(server: ServerProfileEntity)

    @Query("UPDATE server_profiles SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE server_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultServer(id: Long)
}
