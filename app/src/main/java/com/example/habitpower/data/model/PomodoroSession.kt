package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val date: LocalDate,
    val durationMinutes: Int,
    val completedAt: Long,
    val linkedHabitId: Long? = null
)
