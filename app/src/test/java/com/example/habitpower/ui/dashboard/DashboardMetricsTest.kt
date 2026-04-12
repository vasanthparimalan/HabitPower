package com.example.habitpower.ui.dashboard

import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardMetricsTest {

    @Test
    fun consistencyPercentage_countsOnlyScheduledOpportunities() {
        val monday = LocalDate.of(2026, 4, 13)
        val habits = listOf(
            dailyHabit(1),
            weekdayHabit(2, 1 shl 1),
            everyTwoDaysHabit(3, monday)
        )
        val entriesByDate = listOf(
            DailyHabitEntry(1, 1, monday, booleanValue = true),
            DailyHabitEntry(1, 2, monday, booleanValue = true),
            DailyHabitEntry(1, 3, monday, booleanValue = true),
            DailyHabitEntry(1, 1, monday.plusDays(1), booleanValue = true),
            DailyHabitEntry(1, 1, monday.plusDays(2), booleanValue = true)
        ).groupBy { it.date }

        val percentage = DashboardMetrics.consistencyPercentage(
            habits = habits,
            entriesByDate = entriesByDate,
            start = monday,
            end = monday.plusDays(2)
        )

        assertEquals(83, percentage)
    }

    @Test
    fun currentStreak_skipsUnscheduleDaysAndStopsOnMiss() {
        val today = LocalDate.of(2026, 4, 15)
        val habit = weekdayHabit(7, 1 shl 1)
        val entriesByDate = listOf(
            DailyHabitEntry(1, 7, LocalDate.of(2026, 4, 13), booleanValue = true)
        ).groupBy { it.date }

        val streak = DashboardMetrics.currentStreak(
            habits = listOf(habit),
            entriesByDate = entriesByDate,
            today = today
        )

        assertEquals(1, streak)
    }

    @Test
    fun bestWeeklyPercentage_usesScheduledCountsPerWeek() {
        val start = LocalDate.of(2026, 4, 6)
        val habit = weekdayHabit(9, (1 shl 1) or (1 shl 3) or (1 shl 5))
        val entriesByDate = listOf(
            DailyHabitEntry(1, 9, LocalDate.of(2026, 4, 6), booleanValue = true),
            DailyHabitEntry(1, 9, LocalDate.of(2026, 4, 8), booleanValue = true),
            DailyHabitEntry(1, 9, LocalDate.of(2026, 4, 10), booleanValue = true),
            DailyHabitEntry(1, 9, LocalDate.of(2026, 4, 13), booleanValue = true),
            DailyHabitEntry(1, 9, LocalDate.of(2026, 4, 18), booleanValue = true)
        ).groupBy { it.date }

        val best = DashboardMetrics.bestWeeklyPercentage(
            habits = listOf(habit),
            entriesByDate = entriesByDate,
            start = start,
            end = start.plusDays(13)
        )

        assertEquals(100, best)
    }

    @Test
    fun buildHeatmap_returnsZeroRatioWhenNothingScheduled() {
        val date = LocalDate.of(2026, 4, 14)
        val heatmap = DashboardMetrics.buildHeatmap(
            habits = listOf(weekdayHabit(11, 1 shl 1)),
            entriesByDate = emptyMap(),
            start = date,
            end = date
        )

        assertEquals(0f, heatmap[date]?.first)
        assertTrue(heatmap[date]?.second == true)
    }

    private fun dailyHabit(id: Long): HabitDefinition = baseHabit(id)

    private fun weekdayHabit(id: Long, mask: Int): HabitDefinition = baseHabit(
        id = id,
        recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
        recurrenceDaysOfWeekMask = mask
    )

    private fun everyTwoDaysHabit(id: Long, anchor: LocalDate): HabitDefinition = baseHabit(
        id = id,
        recurrenceType = HabitRecurrenceType.EVERY_N_DAYS,
        recurrenceInterval = 2,
        recurrenceAnchorDate = anchor,
        recurrenceStartDate = anchor
    )

    private fun baseHabit(
        id: Long,
        recurrenceType: HabitRecurrenceType = HabitRecurrenceType.DAILY,
        recurrenceDaysOfWeekMask: Int = 0,
        recurrenceInterval: Int = 1,
        recurrenceAnchorDate: LocalDate? = null,
        recurrenceStartDate: LocalDate? = LocalDate.of(2026, 4, 1)
    ): HabitDefinition {
        return HabitDefinition(
            id = id,
            name = "Habit $id",
            goalIdentityStatement = "consistency",
            description = "test",
            commitmentTime = "09:00",
            commitmentLocation = "Home",
            preReminderMinutes = 15,
            recurrenceType = recurrenceType,
            recurrenceDaysOfWeekMask = recurrenceDaysOfWeekMask,
            recurrenceInterval = recurrenceInterval,
            recurrenceAnchorDate = recurrenceAnchorDate,
            recurrenceStartDate = recurrenceStartDate,
            type = HabitType.BOOLEAN
        )
    }
}
