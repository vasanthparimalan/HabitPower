package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chant_sessions")
data class ChantSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val chantId: Long,
    val targetCount: Int,
    val actualCount: Int,
    val durationSeconds: Long,
    val completedAt: Long = System.currentTimeMillis()
)
