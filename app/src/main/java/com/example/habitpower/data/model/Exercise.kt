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
    val targetDurationSeconds: Int?, // if time based
    val targetReps: Int?, // if rep based
    val targetSets: Int?, // Target number of sets
    val notes: String? = null // Important notes for execution
)
