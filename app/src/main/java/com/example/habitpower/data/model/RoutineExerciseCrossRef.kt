package com.example.habitpower.data.model

import androidx.room.Entity

@Entity(primaryKeys = ["routineId", "exerciseId"], tableName = "routine_exercise_cross_ref")
data class RoutineExerciseCrossRef(
    val routineId: Long,
    val exerciseId: Long,
    val order: Int // To maintain the order of exercises in a routine
)
