package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meditation_sessions")
data class MeditationSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val presetName: String,
    val durationSeconds: Long,
    val completedAt: Long = System.currentTimeMillis()
)
