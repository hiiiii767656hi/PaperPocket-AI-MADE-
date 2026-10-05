package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlayerRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_records WHERE serverId = :serverId ORDER BY username ASC")
    fun getAllPlayers(serverId: Long): Flow<List<PlayerRecordEntity>>

    @Query("SELECT * FROM player_records WHERE serverId = :serverId AND isWhitelisted = 1 ORDER BY username ASC")
    fun getWhitelistedPlayers(serverId: Long): Flow<List<PlayerRecordEntity>>

    @Query("SELECT * FROM player_records WHERE serverId = :serverId AND isOp = 1 ORDER BY username ASC")
    fun getOpPlayers(serverId: Long): Flow<List<PlayerRecordEntity>>

    @Query("SELECT * FROM player_records WHERE serverId = :serverId AND isBanned = 1 ORDER BY username ASC")
    fun getBannedPlayers(serverId: Long): Flow<List<PlayerRecordEntity>>

    @Query("SELECT * FROM player_records WHERE serverId = :serverId AND username = :username LIMIT 1")
    suspend fun getPlayerByName(serverId: Long, username: String): PlayerRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(player: PlayerRecordEntity): Long

    @Update
    suspend fun update(player: PlayerRecordEntity)

    @Delete
    suspend fun delete(player: PlayerRecordEntity)

    @Query("DELETE FROM player_records WHERE serverId = :serverId AND username = :username")
    suspend fun deleteByName(serverId: Long, username: String)

    @Query("DELETE FROM player_records WHERE serverId = :serverId")
    suspend fun deleteAllPlayersForServer(serverId: Long)
}
