package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores notification settings for routines (sound preferences for exercise start/end)
 */
@Entity(tableName = "routine_notification_settings")
data class RoutineNotificationSettings(
    @PrimaryKey
    val id: Long = 1, // Single settings row
    val enableExerciseStartSound: Boolean = true,
    val enableExerciseEndSound: Boolean = true,
    val exerciseStartSoundUri: String? = null, // URI to sound file or system sound name
    val exerciseEndSoundUri: String? = null
)
