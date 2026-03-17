package com.example.startupengine.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedStepDao {
    @Query("SELECT * FROM completed_steps WHERE taskId = :taskId ORDER BY stepIndex ASC")
    fun getStepsForTask(taskId: String): Flow<List<CompletedStep>>

    @Query("SELECT * FROM completed_steps WHERE taskId = :taskId ORDER BY stepIndex ASC")
    suspend fun getStepsForTaskOnce(taskId: String): List<CompletedStep>

    @Query("SELECT COUNT(*) FROM completed_steps WHERE taskId = :taskId")
    suspend fun getStepCountForTask(taskId: String): Int

    @Insert
    suspend fun insert(step: CompletedStep)

    @Query("DELETE FROM completed_steps WHERE taskId = :taskId")
    suspend fun deleteStepsForTask(taskId: String)
}
