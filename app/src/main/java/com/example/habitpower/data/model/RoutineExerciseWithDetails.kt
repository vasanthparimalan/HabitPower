package com.example.habitpower.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineExerciseWithDetails(
    @Embedded val crossRef: RoutineExerciseCrossRef,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise
)
