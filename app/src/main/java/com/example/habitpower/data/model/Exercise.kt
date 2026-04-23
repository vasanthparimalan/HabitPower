package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val imageUri: String?,
    val targetDurationSeconds: Int?,
    val targetReps: Int?,
    val targetSets: Int?,
    val notes: String? = null,
    val instructions: String? = null,
    val tags: String = "",
    val category: ExerciseCategory = ExerciseCategory.STRENGTH,
    val wgerExerciseId: Int? = null
)

enum class ExerciseTag {
    GYM,
    BODYWEIGHT,
    SIMPLE_TOOLS,
    YOGA,
    FLEXIBILITY,
    STRETCHES,
    COOLDOWN
}
