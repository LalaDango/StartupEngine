package com.example.startupengine.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
