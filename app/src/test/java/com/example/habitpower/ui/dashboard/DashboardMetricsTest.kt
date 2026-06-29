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

    // ── T1.2 Existing ──────────────────────────────────────────────────────────

    @Test
    fun consistencyPercentage_countsOnlyScheduledOpportunities() {
        val monday = LocalDate.of(2026, 4, 13)
        val habits = listOf(
            dailyHabit(1),
            weekdayHabit(2, 1 shl 1), // Monday only
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
        val habit = weekdayHabit(7, 1 shl 1) // Monday only
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
        val habit = weekdayHabit(9, (1 shl 1) or (1 shl 3) or (1 shl 5)) // Mon, Wed, Fri
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
        val date = LocalDate.of(2026, 4, 14) // Tuesday
        val heatmap = DashboardMetrics.buildHeatmap(
            habits = listOf(weekdayHabit(11, 1 shl 1)), // Monday only
            entriesByDate = emptyMap(),
            start = date,
            end = date
        )

        assertEquals(0f, heatmap[date]?.first)
        assertTrue(heatmap[date]?.second == true)
    }

    // ── T1.2 New: consistency percentage edge cases ───────────────────────────

    @Test
    fun consistencyPercentage_noHabits_returnsZero() {
        val percentage = DashboardMetrics.consistencyPercentage(
            habits = emptyList(),
            entriesByDate = emptyMap(),
            start = LocalDate.of(2026, 6, 1),
            end = LocalDate.of(2026, 6, 7)
        )
        assertEquals(0, percentage)
    }

    @Test
    fun consistencyPercentage_allMissed_returnsZero() {
        val start = LocalDate.of(2026, 6, 1)
        val percentage = DashboardMetrics.consistencyPercentage(
            habits = listOf(dailyHabit(1)),
            entriesByDate = emptyMap(), // no completions
            start = start,
            end = start.plusDays(6)
        )
        assertEquals(0, percentage)
    }

    @Test
    fun consistencyPercentage_100percent() {
        val start = LocalDate.of(2026, 6, 1)
        val end = start.plusDays(6)
        var date = start
        val entries = mutableListOf<DailyHabitEntry>()
        while (!date.isAfter(end)) {
            entries.add(DailyHabitEntry(1, 1, date, booleanValue = true))
            date = date.plusDays(1)
        }
        val percentage = DashboardMetrics.consistencyPercentage(
            habits = listOf(dailyHabit(1)),
            entriesByDate = entries.groupBy { it.date },
            start = start,
            end = end
        )
        assertEquals(100, percentage)
    }

    // ── T1.2 New: streak edge cases ───────────────────────────────────────────

    @Test
    fun currentStreak_noHabits_returnsZero() {
        val streak = DashboardMetrics.currentStreak(
            habits = emptyList(),
            entriesByDate = emptyMap(),
            today = LocalDate.of(2026, 6, 13)
        )
        assertEquals(0, streak)
    }

    @Test
    fun currentStreak_todayIncomplete_doesNotCount() {
        val today = LocalDate.of(2026, 6, 13)
        // Yesterday complete, today no entry
        val entriesByDate = mapOf(
            today.minusDays(1) to listOf(DailyHabitEntry(1, 1, today.minusDays(1), booleanValue = true))
        )
        val streak = DashboardMetrics.currentStreak(
            habits = listOf(dailyHabit(1)),
            entriesByDate = entriesByDate,
            today = today
        )
        assertEquals(0, streak) // today incomplete → streak=0
    }

    @Test
    fun currentStreak_multipleHabits_allMustComplete() {
        val today = LocalDate.of(2026, 6, 13)
        // Habit 1 complete, habit 2 missing today → breaks streak
        val entriesByDate = mapOf(
            today to listOf(DailyHabitEntry(1, 1, today, booleanValue = true))
            // habit 2 has no entry
        )
        val streak = DashboardMetrics.currentStreak(
            habits = listOf(dailyHabit(1), dailyHabit(2)),
            entriesByDate = entriesByDate,
            today = today
        )
        assertEquals(0, streak)
    }

    @Test
    fun currentStreak_30dayStreak() {
        val today = LocalDate.of(2026, 6, 13)
        val entries = mutableListOf<DailyHabitEntry>()
        for (i in 0L..29L) {
            entries.add(DailyHabitEntry(1, 1, today.minusDays(i), booleanValue = true))
        }
        val streak = DashboardMetrics.currentStreak(
            habits = listOf(dailyHabit(1)),
            entriesByDate = entries.groupBy { it.date },
            today = today
        )
        assertEquals(30, streak)
    }

    // ── T1.2 New: heatmap ─────────────────────────────────────────────────────

    @Test
    fun buildHeatmap_allComplete_ratio1f() {
        val date = LocalDate.of(2026, 6, 13) // Saturday
        val allDaysMask = (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4) or
                (1 shl 5) or (1 shl 6) or (1 shl 7)
        val habit = weekdayHabit(1, allDaysMask)
        val entriesByDate = mapOf(date to listOf(DailyHabitEntry(1, 1, date, booleanValue = true)))
        val heatmap = DashboardMetrics.buildHeatmap(
            habits = listOf(habit),
            entriesByDate = entriesByDate,
            start = date,
            end = date
        )
        assertEquals(1f, heatmap[date]?.first)
    }

    @Test
    fun buildHeatmap_halfComplete_ratio0point5() {
        val date = LocalDate.of(2026, 6, 13)
        val allDaysMask = (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4) or
                (1 shl 5) or (1 shl 6) or (1 shl 7)
        // 2 habits scheduled, only 1 completed
        val habits = listOf(weekdayHabit(1, allDaysMask), weekdayHabit(2, allDaysMask))
        val entriesByDate = mapOf(date to listOf(DailyHabitEntry(1, 1, date, booleanValue = true)))
        val heatmap = DashboardMetrics.buildHeatmap(
            habits = habits,
            entriesByDate = entriesByDate,
            start = date,
            end = date
        )
        assertEquals(0.5f, heatmap[date]?.first)
    }

    // ── T1.2 New: bestWeeklyPercentage ────────────────────────────────────────

    @Test
    fun bestWeeklyPercentage_singlePerfectWeek() {
        val start = LocalDate.of(2026, 6, 1)
        // Daily habit, only week 1 fully complete, week 2 empty
        val entries = (0L..6L).map { offset ->
            DailyHabitEntry(1, 1, start.plusDays(offset), booleanValue = true)
        }
        val best = DashboardMetrics.bestWeeklyPercentage(
            habits = listOf(dailyHabit(1)),
            entriesByDate = entries.groupBy { it.date },
            start = start,
            end = start.plusDays(13)
        )
        assertEquals(100, best)
    }

    // ── T1.2 New: scheduledHabitIdsForDate ────────────────────────────────────

    @Test
    fun scheduledHabitIdsForDate_returnsOnlyScheduledIds() {
        val monday = LocalDate.of(2026, 6, 8) // Monday
        val habits = listOf(
            dailyHabit(1),                        // scheduled every day
            weekdayHabit(2, 1 shl 1),             // Monday only → scheduled
            weekdayHabit(3, 1 shl 3)              // Wednesday only → not scheduled
        )
        val ids = DashboardMetrics.scheduledHabitIdsForDate(habits, monday)
        assertTrue(1L in ids)
        assertTrue(2L in ids)
        assertTrue(3L !in ids)
        assertEquals(2, ids.size)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

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
    ): HabitDefinition = HabitDefinition(
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
