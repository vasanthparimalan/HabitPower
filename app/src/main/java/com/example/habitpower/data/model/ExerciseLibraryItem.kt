package com.example.habitpower.data.model

data class ExerciseLibraryItem(
    val name: String,
    val category: ExerciseCategory,
    val primaryMuscle: String?,
    val instructions: String?,
    val imageUri: String?,
    val wgerExerciseId: Int? = null
)
