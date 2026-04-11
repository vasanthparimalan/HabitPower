package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val routineId: Long,
    val routineName: String,
    val isCompleted: Boolean,
    val startTime: Long,
    val endTime: Long?
)
