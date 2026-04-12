package com.example.habitpower.reminder

import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object HabitRecurrenceCalculator {
    private const val MAX_LOOKAHEAD_DAYS = 366 * 6

    fun nextReminderTrigger(
        now: LocalDateTime,
        commitmentTime: LocalTime,
        reminderMinutes: Int,
        habit: HabitDefinition
    ): LocalDateTime? {
        val startDate = habit.recurrenceStartDate ?: LocalDate.MIN
        val endDate = habit.recurrenceEndDate ?: LocalDate.MAX
        val firstDate = maxOf(now.toLocalDate().minusDays(1), startDate)

        for (offset in 0..MAX_LOOKAHEAD_DAYS) {
            val date = firstDate.plusDays(offset.toLong())
            if (date.isAfter(endDate)) return null
            if (!isScheduledOn(date, habit)) continue

            val commitmentDateTime = LocalDateTime.of(date, commitmentTime)
            val trigger = commitmentDateTime.minusMinutes(reminderMinutes.toLong())
            if (trigger.isAfter(now)) {
                return trigger
            }
        }
        return null
    }

    fun isScheduledOn(date: LocalDate, habit: HabitDefinition): Boolean {
        val startDate = habit.recurrenceStartDate
        val endDate = habit.recurrenceEndDate
        if (startDate != null && date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false

        return when (habit.recurrenceType) {
            HabitRecurrenceType.DAILY -> true
            HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> weeklySelectedDaysMatches(date, habit.recurrenceDaysOfWeekMask)
            HabitRecurrenceType.EVERY_N_DAYS -> everyNDaysMatches(date, habit)
            HabitRecurrenceType.MONTHLY_BY_DATE -> date.dayOfMonth == habit.recurrenceDayOfMonth
            HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> monthlyNthWeekdayMatches(date, habit)
            HabitRecurrenceType.YEARLY_BY_DATE -> yearlyByDateMatches(date, habit.recurrenceYearlyDates)
            HabitRecurrenceType.YEARLY_MULTI_DATE -> yearlyByDateMatches(date, habit.recurrenceYearlyDates)
        }
    }

    private fun weeklySelectedDaysMatches(date: LocalDate, dayMask: Int): Boolean {
        if (dayMask == 0) return false
        val bit = 1 shl date.dayOfWeek.value
        return (dayMask and bit) != 0
    }

    private fun everyNDaysMatches(date: LocalDate, habit: HabitDefinition): Boolean {
        val interval = habit.recurrenceInterval.coerceAtLeast(1)
        val anchor = habit.recurrenceAnchorDate ?: habit.recurrenceStartDate ?: LocalDate.now()
        if (date.isBefore(anchor)) return false
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(anchor, date)
        return daysDiff % interval.toLong() == 0L
    }

    private fun monthlyNthWeekdayMatches(date: LocalDate, habit: HabitDefinition): Boolean {
        val weekOfMonth = habit.recurrenceWeekOfMonth ?: return false
        val weekdayInt = habit.recurrenceWeekday ?: return false
        if (weekdayInt !in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value) return false
        val weekday = DayOfWeek.of(weekdayInt)
        if (date.dayOfWeek != weekday) return false

        val monthDates = mutableListOf<LocalDate>()
        var cursor = date.withDayOfMonth(1)
        while (cursor.month == date.month) {
            if (cursor.dayOfWeek == weekday) {
                monthDates.add(cursor)
            }
            cursor = cursor.plusDays(1)
        }

        if (monthDates.isEmpty()) return false
        val target = if (weekOfMonth == -1) {
            monthDates.last()
        } else {
            val index = weekOfMonth - 1
            if (index !in monthDates.indices) return false
            monthDates[index]
        }
        return date == target
    }

    private fun yearlyByDateMatches(date: LocalDate, datesCsv: String): Boolean {
        if (datesCsv.isBlank()) return false
        val key = String.format("%02d-%02d", date.monthValue, date.dayOfMonth)
        return datesCsv
            .split(',')
            .asSequence()
            .map { it.trim() }
            .filter { it.length == 5 && it[2] == '-' }
            .any { it == key }
    }
}
