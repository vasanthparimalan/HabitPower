package com.example.habitpower.ui.dashboard

import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for habit health detection logic.
 *
 * The algorithm (mirrors DashboardViewModel.strugglingHabits):
 *   - Only ACTIVE habits considered
 *   - Window: last 14 days (today-14 .. today-1, inclusive)
 *   - A habit is "struggling" when: scheduledCount >= 4 AND completedCount * 2 < scheduledCount
 *   - i.e., less than 50% completion over the window
 */
class HabitHealthDetectionTest {

    private val today = LocalDate.of(2026, 6, 13)
    private val windowStart = today.minusDays(14)
    private val windowEnd = today.minusDays(1)

    // ── Threshold detection ────────────────────────────────────────────────────

    @Test
    fun belowThreshold_lessThan4Scheduled_notFlagged() {
        // Daily habit, only 3 days in window by bounding start date
        val habit = dailyHabit(1, startDate = today.minusDays(3))
        val entries = emptyList<DailyHabitEntry>()
        val result = detectStruggling(listOf(habit), entries)
        assertTrue("Habit with <4 scheduled days must not be flagged", result.isEmpty())
    }

    @Test
    fun belowThreshold_exactly50percent_notFlagged() {
        // Daily for 14 days → scheduledCount=14, completedCount=7 → 7*2 == 14 → NOT < 14 → not flagged
        val habit = dailyHabit(1)
        val entries = (0L..6L).map { offset ->
            entry(1, windowStart.plusDays(offset))
        }
        val result = detectStruggling(listOf(habit), entries)
        assertTrue("Exactly 50% must not be flagged (boundary — must exceed, not meet)", result.isEmpty())
    }

    @Test
    fun flagged_0percentOf4scheduled() {
        // Daily 14 days, zero completions → 0 * 2 = 0 < 14 → flagged
        val habit = dailyHabit(1)
        val result = detectStruggling(listOf(habit), emptyList())
        assertEquals(1, result.size)
        assertEquals(0, result.first().completionPercent)
    }

    @Test
    fun flagged_25percentOf8scheduled() {
        // Weekly Mon+Tue+Wed+Thu habit over 2 weeks = 8 scheduled, complete 2 → 2*2=4 < 8 → flagged
        // 2026-06-13 is Saturday. Window end = June 12 (Fri). Window start = May 30 (Sat).
        // Mon-Thu in window: Jun 1,2,3,4, Jun 8,9,10,11 = 8 days
        val mask = (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4) // Mon-Thu
        val habit = weeklyHabit(1, mask)
        val entries = listOf(
            entry(1, LocalDate.of(2026, 6, 1)),
            entry(1, LocalDate.of(2026, 6, 2))
        )
        val result = detectStruggling(listOf(habit), entries)
        assertEquals(1, result.size)
        assertEquals(25, result.first().completionPercent)
    }

    @Test
    fun notFlagged_51percentOf8scheduled() {
        // Same 8-day schedule as above, but 5 completed → 5*2=10 > 8 → not flagged (62%)
        val mask = (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)
        val habit = weeklyHabit(1, mask)
        val entries = listOf(
            entry(1, LocalDate.of(2026, 6, 1)),
            entry(1, LocalDate.of(2026, 6, 2)),
            entry(1, LocalDate.of(2026, 6, 3)),
            entry(1, LocalDate.of(2026, 6, 4)),
            entry(1, LocalDate.of(2026, 6, 8))
        )
        val result = detectStruggling(listOf(habit), entries)
        assertTrue(result.isEmpty())
    }

    // ── Lifecycle exclusions ───────────────────────────────────────────────────

    @Test
    fun pausedHabit_excluded() {
        val habit = dailyHabit(1, lifecycle = HabitLifecycleStatus.PAUSED)
        val result = detectStruggling(listOf(habit), emptyList())
        assertTrue("PAUSED habit must never appear in struggling list", result.isEmpty())
    }

    @Test
    fun graduatedHabit_excluded() {
        val habit = dailyHabit(1, lifecycle = HabitLifecycleStatus.GRADUATED)
        val result = detectStruggling(listOf(habit), emptyList())
        assertTrue("GRADUATED habit must never appear in struggling list", result.isEmpty())
    }

    // ── StrugglingHabit data class ─────────────────────────────────────────────

    @Test
    fun completionPercent_calculatesCorrectly() {
        val struggling = StrugglingHabit(
            habit = dailyHabit(1),
            scheduledCount = 8,
            completedCount = 3
        )
        assertEquals(37, struggling.completionPercent)
    }

    // ── isEntryCompleted per type ──────────────────────────────────────────────

    @Test
    fun isEntryCompleted_boolean_trueOnlyWhenTrue() {
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, booleanValue = true), HabitType.BOOLEAN))
        assertFalse(isEntryCompleted(DailyHabitEntry(1, 1, today, booleanValue = false), HabitType.BOOLEAN))
        assertFalse(isEntryCompleted(DailyHabitEntry(1, 1, today, booleanValue = null), HabitType.BOOLEAN))
    }

    @Test
    fun isEntryCompleted_numeric_anyValueCounts() {
        // Any non-null numeric value counts as completed (threshold checked at display level)
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 0.0), HabitType.NUMBER))
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 5.5), HabitType.NUMBER))
        assertFalse(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = null), HabitType.NUMBER))
        // Same rule for all numeric subtypes
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 1.0), HabitType.DURATION))
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 1.0), HabitType.COUNT))
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 1.0), HabitType.TIME))
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 1.0), HabitType.POMODORO))
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, numericValue = 1.0), HabitType.TIMER))
    }

    @Test
    fun isEntryCompleted_text_blankNotCompleted() {
        assertTrue(isEntryCompleted(DailyHabitEntry(1, 1, today, textValue = "done"), HabitType.TEXT))
        assertFalse(isEntryCompleted(DailyHabitEntry(1, 1, today, textValue = ""), HabitType.TEXT))
        assertFalse(isEntryCompleted(DailyHabitEntry(1, 1, today, textValue = "   "), HabitType.TEXT))
        assertFalse(isEntryCompleted(DailyHabitEntry(1, 1, today, textValue = null), HabitType.TEXT))
    }

    // ── Helpers: mirror DashboardViewModel logic exactly ──────────────────────

    private fun detectStruggling(
        habits: List<HabitDefinition>,
        entries: List<DailyHabitEntry>
    ): List<StrugglingHabit> {
        val activeHabits = habits.filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }
        val entriesByHabitAndDate = entries.groupBy { it.habitId to it.date }

        return activeHabits.mapNotNull { habit ->
            var scheduledCount = 0
            var completedCount = 0
            var date = windowStart
            while (!date.isAfter(windowEnd)) {
                if (HabitRecurrenceCalculator.isScheduledOn(date, habit)) {
                    scheduledCount++
                    val entry = entriesByHabitAndDate[habit.id to date]?.firstOrNull()
                    if (entry != null && isEntryCompleted(entry, habit.type)) completedCount++
                }
                date = date.plusDays(1)
            }
            if (scheduledCount >= 4 && completedCount * 2 < scheduledCount) {
                StrugglingHabit(habit, scheduledCount, completedCount)
            } else null
        }
    }

    private fun isEntryCompleted(entry: DailyHabitEntry, type: HabitType): Boolean = when (type) {
        HabitType.BOOLEAN, HabitType.ROUTINE -> entry.booleanValue == true
        HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT,
        HabitType.POMODORO, HabitType.TIMER, HabitType.TIME -> entry.numericValue != null
        HabitType.TEXT -> !entry.textValue.isNullOrBlank()
    }

    private fun entry(habitId: Long, date: LocalDate) =
        DailyHabitEntry(userId = 1, habitId = habitId, date = date, booleanValue = true)

    private fun dailyHabit(
        id: Long,
        startDate: LocalDate = windowStart,
        lifecycle: HabitLifecycleStatus = HabitLifecycleStatus.ACTIVE
    ) = HabitDefinition(
        id = id,
        name = "Habit $id",
        goalIdentityStatement = "",
        description = "",
        type = HabitType.BOOLEAN,
        recurrenceType = HabitRecurrenceType.DAILY,
        recurrenceStartDate = startDate,
        lifecycleStatus = lifecycle
    )

    private fun weeklyHabit(
        id: Long,
        mask: Int,
        lifecycle: HabitLifecycleStatus = HabitLifecycleStatus.ACTIVE
    ) = HabitDefinition(
        id = id,
        name = "Habit $id",
        goalIdentityStatement = "",
        description = "",
        type = HabitType.BOOLEAN,
        recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
        recurrenceDaysOfWeekMask = mask,
        recurrenceStartDate = windowStart,
        lifecycleStatus = lifecycle
    )
}
