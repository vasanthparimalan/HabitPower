package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val imageUri: String?, // Can be a resource ID or file path
    val targetDurationSeconds: Int?, // if time based, primary duration field
    val targetReps: Int?, // if rep based
    val targetSets: Int?, // Target number of sets
    val notes: String? = null, // Important notes for execution
    val instructions: String? = null, // Step-by-step instructions (can be numbered or unnumbered)
    val tags: String = "" // CSV format: "gym,bodyweight,yoga" or use ExerciseTag table via DAO
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
