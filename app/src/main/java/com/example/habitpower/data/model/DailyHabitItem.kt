package com.example.habitpower.data.model

import androidx.room.ColumnInfo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DailyHabitItem(
    @ColumnInfo(name = "habitId")
    val habitId: Long,
    @ColumnInfo(name = "routineId")
    val routineId: Long?,
    @ColumnInfo(name = "lifeAreaId")
    val lifeAreaId: Long?,
    val name: String,
    @ColumnInfo(name = "goalIdentityStatement")
    val goalIdentityStatement: String = "",
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
    val operator: TargetOperator,
    val recurrenceType: HabitRecurrenceType = HabitRecurrenceType.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeekMask: Int = 0,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceWeekOfMonth: Int? = null,
    val recurrenceWeekday: Int? = null,
    val recurrenceYearlyDates: String = "",
    val recurrenceAnchorDate: LocalDate? = null,
    val recurrenceStartDate: LocalDate? = null,
    val recurrenceEndDate: LocalDate? = null,
    @ColumnInfo(name = "commitmentTime")
    val commitmentTime: String? = null,
    @ColumnInfo(name = "commitmentLocation")
    val commitmentLocation: String = ""
) {
    val effectiveDisplayOrder: Int
        get() = if (assignmentDisplayOrder >= 0) assignmentDisplayOrder else habitDisplayOrder

    fun isScheduledOn(date: LocalDate): Boolean {
        if (recurrenceStartDate != null && date.isBefore(recurrenceStartDate)) return false
        if (recurrenceEndDate != null && date.isAfter(recurrenceEndDate)) return false

        return when (recurrenceType) {
            HabitRecurrenceType.DAILY -> true
            HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> {
                if (recurrenceDaysOfWeekMask == 0) {
                    false
                } else {
                    (recurrenceDaysOfWeekMask and (1 shl date.dayOfWeek.value)) != 0
                }
            }

            HabitRecurrenceType.EVERY_N_DAYS -> {
                val interval = recurrenceInterval.coerceAtLeast(1)
                val anchor = recurrenceAnchorDate ?: recurrenceStartDate ?: LocalDate.now()
                !date.isBefore(anchor) && ChronoUnit.DAYS.between(anchor, date) % interval.toLong() == 0L
            }

            HabitRecurrenceType.MONTHLY_BY_DATE -> date.dayOfMonth == recurrenceDayOfMonth
            HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                val weekOfMonth = recurrenceWeekOfMonth
                val weekday = recurrenceWeekday
                if (weekOfMonth == null || weekday == null || weekday !in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value) {
                    false
                } else if (date.dayOfWeek.value != weekday) {
                    false
                } else {
                    val candidates = mutableListOf<LocalDate>()
                    var cursor = date.withDayOfMonth(1)
                    while (cursor.month == date.month) {
                        if (cursor.dayOfWeek.value == weekday) {
                            candidates.add(cursor)
                        }
                        cursor = cursor.plusDays(1)
                    }
                    val target = if (weekOfMonth == -1) {
                        candidates.lastOrNull()
                    } else {
                        candidates.getOrNull(weekOfMonth - 1)
                    }
                    date == target
                }
            }

            HabitRecurrenceType.YEARLY_BY_DATE,
            HabitRecurrenceType.YEARLY_MULTI_DATE -> {
                if (recurrenceYearlyDates.isBlank()) {
                    false
                } else {
                    val key = String.format("%02d-%02d", date.monthValue, date.dayOfMonth)
                    recurrenceYearlyDates
                        .split(',')
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.length == 5 && it[2] == '-' }
                        .any { it == key }
                }
            }
        }
    }
}
