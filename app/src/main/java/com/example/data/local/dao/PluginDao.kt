package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InstalledPluginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {
    @Query("SELECT * FROM installed_plugins WHERE serverId = :serverId ORDER BY name ASC")
    fun getPluginsForServer(serverId: Long): Flow<List<InstalledPluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: InstalledPluginEntity): Long

    @Update
    suspend fun updatePlugin(plugin: InstalledPluginEntity)

    @Delete
    suspend fun deletePlugin(plugin: InstalledPluginEntity)

    @Query("DELETE FROM installed_plugins WHERE serverId = :serverId AND pluginId = :pluginId")
    suspend fun deletePluginById(serverId: Long, pluginId: String)

    @Query("UPDATE installed_plugins SET isEnabled = :enabled WHERE id = :id")
    suspend fun setPluginEnabled(id: Long, enabled: Boolean)
}
