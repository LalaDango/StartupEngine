package com.example.startupengine.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalStatsDao {
    @Query("SELECT * FROM global_stats WHERE id = 1")
    fun getStats(): Flow<GlobalStats?>

    @Query("SELECT * FROM global_stats WHERE id = 1")
    suspend fun getStatsOnce(): GlobalStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: GlobalStats)

    @Query("UPDATE global_stats SET totalSteps = totalSteps + 1 WHERE id = 1")
    suspend fun incrementTotalSteps()

    @Query("UPDATE global_stats SET totalTasks = totalTasks + 1 WHERE id = 1")
    suspend fun incrementTotalTasks()
}
