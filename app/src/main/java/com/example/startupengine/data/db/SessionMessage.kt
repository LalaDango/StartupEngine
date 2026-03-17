package com.example.startupengine.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_messages",
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class SessionMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: String,
    val text: String,
    val isAI: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
