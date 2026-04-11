package com.example.habitpower.data.model

import androidx.room.ColumnInfo

data class DailyHabitItem(
    @ColumnInfo(name = "habitId")
    val habitId: Long,
    @ColumnInfo(name = "lifeAreaId")
    val lifeAreaId: Long?,
    val name: String,
    val description: String,
    val type: HabitType,
    val unit: String?,
    val targetValue: Double?,
    val showInWidget: Boolean,
    val showInDailyCheckIn: Boolean,
    @ColumnInfo(name = "habitDisplayOrder")
    val habitDisplayOrder: Int,
    @ColumnInfo(name = "assignmentDisplayOrder")
    val assignmentDisplayOrder: Int,
    @ColumnInfo(name = "entryBooleanValue")
    val entryBooleanValue: Boolean?,
    @ColumnInfo(name = "entryNumericValue")
    val entryNumericValue: Double?,
    @ColumnInfo(name = "entryTextValue")
    val entryTextValue: String?,
    val operator: TargetOperator
) {
    val effectiveDisplayOrder: Int
        get() = if (assignmentDisplayOrder >= 0) assignmentDisplayOrder else habitDisplayOrder
}
