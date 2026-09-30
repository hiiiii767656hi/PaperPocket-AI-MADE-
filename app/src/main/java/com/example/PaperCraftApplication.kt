package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ServerRepository
import com.example.server.ServerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaperCraftApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ServerRepository
        private set

    lateinit var serverEngine: ServerEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = ServerRepository(this, database)
        serverEngine = ServerEngine(this)

        // Seed initial data if necessary
        CoroutineScope(Dispatchers.IO).launch {
            repository.ensureDefaultData()
        }
    }
}
