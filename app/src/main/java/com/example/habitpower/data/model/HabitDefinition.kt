package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_definitions")
data class HabitDefinition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val goalIdentityStatement: String = "",
    val description: String,
    val commitmentTime: String? = null,
    val commitmentLocation: String = "",
    val preReminderMinutes: Int? = null,
    val type: HabitType,
    val unit: String? = null,
    val targetValue: Double? = null,
    val showInWidget: Boolean = true,
    val showInDailyCheckIn: Boolean = true,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val operator: TargetOperator = TargetOperator.GREATER_THAN_OR_EQUAL,
    val lifeAreaId: Long? = null
)
