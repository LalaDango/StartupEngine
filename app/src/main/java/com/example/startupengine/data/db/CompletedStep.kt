package com.example.startupengine.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completed_steps",
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class CompletedStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: String,
    val stepText: String,
    val stepIndex: Int,
    val completedAt: Long
)
