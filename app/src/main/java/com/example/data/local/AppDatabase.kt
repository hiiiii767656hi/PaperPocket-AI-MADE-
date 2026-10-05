package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BackupDao
import com.example.data.local.dao.PlayerDao
import com.example.data.local.dao.PluginDao
import com.example.data.local.dao.ServerDao
import com.example.data.local.entity.BackupEntity
import com.example.data.local.entity.InstalledPluginEntity
import com.example.data.local.entity.PlayerRecordEntity
import com.example.data.local.entity.ServerProfileEntity

@Database(
    entities = [
        ServerProfileEntity::class,
        InstalledPluginEntity::class,
        PlayerRecordEntity::class,
        BackupEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun pluginDao(): PluginDao
    abstract fun playerDao(): PlayerDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "papercraft_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
