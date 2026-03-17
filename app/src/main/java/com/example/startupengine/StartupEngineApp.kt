package com.example.startupengine

import android.app.Application
import com.example.startupengine.data.db.AppDatabase
import com.example.startupengine.data.repository.AIRepository
import com.example.startupengine.data.repository.SettingsRepository
import com.example.startupengine.data.repository.TaskRepository

class StartupEngineApp : Application() {
    lateinit var database: AppDatabase
    lateinit var settingsRepository: SettingsRepository
    lateinit var taskRepository: TaskRepository
    lateinit var aiRepository: AIRepository

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        taskRepository = TaskRepository(
            database.taskDao(),
            database.completedStepDao(),
            database.globalStatsDao(),
            database.sessionMessageDao()
        )
        aiRepository = AIRepository(settingsRepository)
    }
}
