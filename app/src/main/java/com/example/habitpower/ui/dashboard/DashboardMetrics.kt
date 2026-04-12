package com.example.habitpower.ui.dashboard

import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import java.time.LocalDate

object DashboardMetrics {
    fun buildHeatmap(
        habits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        start: LocalDate,
        end: LocalDate
    ): Map<LocalDate, Pair<Float, Boolean>> {
        val hasHabits = habits.isNotEmpty()
        val heatmap = mutableMapOf<LocalDate, Pair<Float, Boolean>>()

        var date = start
        while (!date.isAfter(end)) {
            val dayEntries = entriesByDate[date].orEmpty()
            val scheduledHabitIds = scheduledHabitIdsForDate(habits, date)
            val completedIds = dayEntries.map { it.habitId }.distinct().toSet()
            val points = scheduledHabitIds.count { it in completedIds }.toFloat()
            val ratio = if (scheduledHabitIds.isNotEmpty()) {
                (points / scheduledHabitIds.size.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            heatmap[date] = ratio to hasHabits
            date = date.plusDays(1)
        }

        return heatmap
    }

    fun currentStreak(
        habits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        today: LocalDate
    ): Int {
        if (habits.isEmpty()) return 0

        var streak = 0
        var date = today
        var guard = 0
        while (guard < 3650) {
            val completedHabits = entriesByDate[date].orEmpty().map { it.habitId }.distinct().toSet()
            val scheduledHabitIds = scheduledHabitIdsForDate(habits, date)

            if (scheduledHabitIds.isEmpty()) {
                date = date.minusDays(1)
                guard++
                continue
            }

            if (scheduledHabitIds.all { it in completedHabits }) {
                streak++
                date = date.minusDays(1)
                guard++
            } else {
                break
            }
        }
        return streak
    }

    fun consistencyPercentage(
        habits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        start: LocalDate,
        end: LocalDate
    ): Int {
        if (habits.isEmpty()) return 0

        var completedCount = 0
        var totalCount = 0
        var date = start
        while (!date.isAfter(end)) {
            val completedHabits = entriesByDate[date].orEmpty().map { it.habitId }.distinct().toSet()
            val scheduledHabitIds = scheduledHabitIdsForDate(habits, date)
            completedCount += scheduledHabitIds.count { it in completedHabits }
            totalCount += scheduledHabitIds.size
            date = date.plusDays(1)
        }

        return if (totalCount == 0) 0 else ((completedCount * 100) / totalCount)
    }

    fun bestWeeklyPercentage(
        habits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        start: LocalDate,
        end: LocalDate
    ): Int {
        if (habits.isEmpty()) return 0

        var bestWeekPercentage = 0
        var weekStart = start
        while (!weekStart.isAfter(end)) {
            val weekEnd = weekStart.plusDays(6).let { if (it.isAfter(end)) end else it }
            val percentage = consistencyPercentage(habits, entriesByDate, weekStart, weekEnd)
            if (percentage > bestWeekPercentage) {
                bestWeekPercentage = percentage
            }
            weekStart = weekStart.plusDays(7)
        }
        return bestWeekPercentage
    }

    fun scheduledHabitIdsForDate(habits: List<HabitDefinition>, date: LocalDate): Set<Long> {
        return habits
            .asSequence()
            .filter { HabitRecurrenceCalculator.isScheduledOn(date, it) }
            .map { it.id }
            .toSet()
    }
}