package com.example.startupengine.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionMessageDao {
    @Query("SELECT * FROM session_messages WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun getMessagesForTask(taskId: String): List<SessionMessage>

    @Insert
    suspend fun insert(message: SessionMessage)

    @Query("DELETE FROM session_messages WHERE taskId = :taskId")
    suspend fun deleteMessagesForTask(taskId: String)
}
