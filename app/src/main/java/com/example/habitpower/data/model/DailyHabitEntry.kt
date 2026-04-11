package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "daily_habit_entries",
    primaryKeys = ["userId", "habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HabitDefinition::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("date")]
)
data class DailyHabitEntry(
    val userId: Long,
    val habitId: Long,
    val date: LocalDate,
    val booleanValue: Boolean? = null,
    val numericValue: Double? = null,
    val textValue: String? = null
)
