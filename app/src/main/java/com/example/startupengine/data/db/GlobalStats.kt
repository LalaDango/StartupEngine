package com.example.startupengine.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "global_stats")
data class GlobalStats(
    @PrimaryKey val id: Int = 1,
    val totalSteps: Int = 0,
    val totalTasks: Int = 0
)
