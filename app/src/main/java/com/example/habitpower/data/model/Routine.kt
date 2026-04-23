package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RoutineType(val displayName: String) {
    NORMAL("Normal"),
    TIMED("Timed")
}

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val type: RoutineType = RoutineType.NORMAL,
    val restTimeSeconds: Int = 0,
    /** How many times the full exercise list repeats. 1 = single pass (default). */
    val repeatCount: Int = 1
)
