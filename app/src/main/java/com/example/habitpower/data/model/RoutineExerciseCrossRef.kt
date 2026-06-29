package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_exercise_cross_ref")
data class RoutineExerciseCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val order: Int,
    val sets: Int? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null
)
