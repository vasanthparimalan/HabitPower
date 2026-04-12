package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RoutineType {
    NORMAL,      // User marks exercises complete, shows next one
    TIMED        // Auto-switch with exercise duration + rest time
}

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val type: RoutineType = RoutineType.NORMAL, // Type of routine
    val restTimeSeconds: Int = 0 // Rest time between exercises in timed routines
)
