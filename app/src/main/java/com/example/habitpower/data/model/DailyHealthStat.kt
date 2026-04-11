package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_health_stats")
data class DailyHealthStat(
    @PrimaryKey
    val date: LocalDate,
    val sleepHours: Float,
    val stepsCount: Int,
    val meditationCompleted: Boolean = false
)
