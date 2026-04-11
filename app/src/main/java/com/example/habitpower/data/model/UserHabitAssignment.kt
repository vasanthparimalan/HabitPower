package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_habit_assignments",
    primaryKeys = ["userId", "habitId"],
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
    indices = [Index("habitId")]
)
data class UserHabitAssignment(
    val userId: Long,
    val habitId: Long,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)
