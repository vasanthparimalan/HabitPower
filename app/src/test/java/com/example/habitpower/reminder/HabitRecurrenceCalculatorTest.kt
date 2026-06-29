package com.example.habitpower.reminder

import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HabitRecurrenceCalculatorTest {

    // ── T1.1 Existing ──────────────────────────────────────────────────────────

    @Test
    fun weeklySelectedDays_matchesOnlyChosenWeekdays() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
            recurrenceDaysOfWeekMask = (1 shl 1) or (1 shl 3) or (1 shl 5) // Mon, Wed, Fri
        )
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 13), habit)) // Mon
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 15), habit)) // Wed
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 17), habit)) // Fri
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 14), habit)) // Tue
    }

    @Test
    fun everyNDays_usesAnchorDate() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.EVERY_N_DAYS,
            recurrenceInterval = 14,
            recurrenceAnchorDate = LocalDate.of(2026, 4, 1),
            recurrenceStartDate = LocalDate.of(2026, 4, 1)
        )
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 1), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 15), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 14), habit))
    }

    @Test
    fun monthlyByDate_skipsInvalidShortMonthDate() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_DATE,
            recurrenceDayOfMonth = 31,
            recurrenceStartDate = LocalDate.of(2026, 1, 31)
        )
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 30), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 5, 31), habit))
    }

    @Test
    fun monthlyNthWeekday_supportsLastWeekdayOfMonth() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY,
            recurrenceWeekOfMonth = -1,
            recurrenceWeekday = 2, // Tuesday
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 28), habit))  // last Tue of April
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 21), habit)) // earlier Tue
    }

    @Test
    fun yearlyMultiDate_matchesConfiguredDatesOnly() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.YEARLY_MULTI_DATE,
            recurrenceYearlyDates = "01-01,07-01",
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 1, 1), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 7, 1), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 7, 2), habit))
    }

    @Test
    fun nextReminderTrigger_returnsNextValidOccurrenceForSkippedMonthlyDate() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_DATE,
            recurrenceDayOfMonth = 31,
            recurrenceStartDate = LocalDate.of(2026, 1, 31)
        )
        val trigger = HabitRecurrenceCalculator.nextReminderTrigger(
            now = LocalDateTime.of(2026, 4, 15, 10, 0),
            commitmentTime = LocalTime.of(9, 0),
            reminderMinutes = 30,
            habit = habit
        )
        assertNotNull(trigger)
        assertEquals(LocalDateTime.of(2026, 5, 31, 8, 30), trigger)
    }

    // ── T1.1 New: start/end date boundaries ───────────────────────────────────

    @Test
    fun daily_withinStartEndDate_scheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.DAILY,
            recurrenceStartDate = LocalDate.of(2026, 6, 1),
            recurrenceEndDate = LocalDate.of(2026, 6, 30)
        )
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 1), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 15), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 30), habit))
    }

    @Test
    fun daily_beforeStartDate_notScheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.DAILY,
            recurrenceStartDate = LocalDate.of(2026, 6, 10)
        )
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 9), habit))
    }

    @Test
    fun daily_afterEndDate_notScheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.DAILY,
            recurrenceStartDate = LocalDate.of(2026, 1, 1),
            recurrenceEndDate = LocalDate.of(2026, 6, 10)
        )
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 11), habit))
    }

    // ── T1.1 New: weekly edge cases ────────────────────────────────────────────

    @Test
    fun weeklySelectedDays_noMaskBits_neverScheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
            recurrenceDaysOfWeekMask = 0
        )
        // Mask=0 means no days selected — should never fire
        for (dayOffset in 0L..6L) {
            assertFalse(
                HabitRecurrenceCalculator.isScheduledOn(
                    LocalDate.of(2026, 6, 8).plusDays(dayOffset), habit
                )
            )
        }
    }

    @Test
    fun weeklySelectedDays_allBitsSet_everyDay() {
        // bits 1-7 = Mon-Sun
        val allDaysMask = (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4) or
                (1 shl 5) or (1 shl 6) or (1 shl 7)
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
            recurrenceDaysOfWeekMask = allDaysMask
        )
        for (dayOffset in 0L..6L) {
            assertTrue(
                HabitRecurrenceCalculator.isScheduledOn(
                    LocalDate.of(2026, 6, 8).plusDays(dayOffset), habit
                )
            )
        }
    }

    // ── T1.1 New: every-N-days edge case ──────────────────────────────────────

    @Test
    fun everyNDays_beforeAnchor_notScheduled() {
        val anchor = LocalDate.of(2026, 6, 1)
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.EVERY_N_DAYS,
            recurrenceInterval = 3,
            recurrenceAnchorDate = anchor,
            recurrenceStartDate = anchor
        )
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 5, 31), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 1), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 4), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 6, 5), habit))
    }

    // ── T1.1 New: monthly-by-date leap year ───────────────────────────────────

    @Test
    fun monthlyByDate_feb29_leapYear_scheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_DATE,
            recurrenceDayOfMonth = 29,
            recurrenceStartDate = LocalDate.of(2028, 1, 1)
        )
        // 2028 is a leap year — Feb 29 exists
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2028, 2, 29), habit))
    }

    @Test
    fun monthlyByDate_feb29_nonLeapYear_notScheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_DATE,
            recurrenceDayOfMonth = 29,
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )
        // Feb 2026 only has 28 days — day 29 never matches any day in Feb
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 2, 28), habit))
    }

    // ── T1.1 New: monthly Nth weekday ─────────────────────────────────────────

    @Test
    fun monthlyNthWeekday_firstWeekdayOfMonth() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY,
            recurrenceWeekOfMonth = 1,
            recurrenceWeekday = 1, // Monday
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )
        // April 2026: first Monday is April 6
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 6), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 13), habit)) // 2nd Monday
    }

    @Test
    fun monthlyNthWeekday_fifthWeekdayWhenOnly4Exist_notScheduled() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY,
            recurrenceWeekOfMonth = 5,
            recurrenceWeekday = 1, // Monday
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )
        // April 2026 has exactly 4 Mondays (6, 13, 20, 27) — no 5th Monday
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 27), habit))
    }

    // ── T1.1 New: yearly by date ──────────────────────────────────────────────

    @Test
    fun yearlyByDate_matches_correctDate() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.YEARLY_BY_DATE,
            recurrenceYearlyDates = "04-15",
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 15), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 16), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2027, 4, 15), habit)) // next year
    }

    // ── T1.1 New: nextReminderTrigger ─────────────────────────────────────────

    @Test
    fun nextTrigger_commitmentTimePassedToday_returnsNextOccurrence() {
        // Daily habit, commitment 09:00, reminder 30min — it's already 10:00
        val habit = testHabit(recurrenceType = HabitRecurrenceType.DAILY)
        val trigger = HabitRecurrenceCalculator.nextReminderTrigger(
            now = LocalDateTime.of(2026, 6, 13, 10, 0),
            commitmentTime = LocalTime.of(9, 0),
            reminderMinutes = 30,
            habit = habit
        )
        // Today's trigger (08:30) is in the past → next is tomorrow at 08:30
        assertEquals(LocalDateTime.of(2026, 6, 14, 8, 30), trigger)
    }

    @Test
    fun nextTrigger_habitWithEndDatePassed_returnsNull() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.DAILY,
            recurrenceStartDate = LocalDate.of(2026, 1, 1),
            recurrenceEndDate = LocalDate.of(2026, 6, 12) // yesterday
        )
        val trigger = HabitRecurrenceCalculator.nextReminderTrigger(
            now = LocalDateTime.of(2026, 6, 13, 8, 0),
            commitmentTime = LocalTime.of(9, 0),
            reminderMinutes = 30,
            habit = habit
        )
        assertNull(trigger)
    }

    @Test
    fun nextTrigger_weeklyHabit_skipsUnscheduledDays() {
        // Monday-only habit, queried on Thursday
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
            recurrenceDaysOfWeekMask = 1 shl 1 // Monday
        )
        // 2026-06-11 is a Thursday
        val trigger = HabitRecurrenceCalculator.nextReminderTrigger(
            now = LocalDateTime.of(2026, 6, 11, 10, 0),
            commitmentTime = LocalTime.of(9, 0),
            reminderMinutes = 30,
            habit = habit
        )
        // Next Monday is June 15, trigger at 08:30
        assertEquals(LocalDateTime.of(2026, 6, 15, 8, 30), trigger)
    }

    @Test
    fun nextTrigger_reminderMinutesZero_exactCommitmentTime() {
        val habit = testHabit(recurrenceType = HabitRecurrenceType.DAILY)
        val trigger = HabitRecurrenceCalculator.nextReminderTrigger(
            now = LocalDateTime.of(2026, 6, 13, 8, 0),
            commitmentTime = LocalTime.of(9, 0),
            reminderMinutes = 0,
            habit = habit
        )
        // No offset — triggers exactly at 09:00 today (still in the future)
        assertEquals(LocalDateTime.of(2026, 6, 13, 9, 0), trigger)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun testHabit(
        recurrenceType: HabitRecurrenceType = HabitRecurrenceType.DAILY,
        recurrenceInterval: Int = 1,
        recurrenceDaysOfWeekMask: Int = 0,
        recurrenceDayOfMonth: Int? = null,
        recurrenceWeekOfMonth: Int? = null,
        recurrenceWeekday: Int? = null,
        recurrenceYearlyDates: String = "",
        recurrenceAnchorDate: LocalDate? = null,
        recurrenceStartDate: LocalDate? = null,
        recurrenceEndDate: LocalDate? = null
    ): HabitDefinition = HabitDefinition(
        id = 1L,
        name = "Test Habit",
        goalIdentityStatement = "show up consistently",
        description = "test",
        commitmentTime = "09:00",
        commitmentLocation = "Home",
        preReminderMinutes = 30,
        recurrenceType = recurrenceType,
        recurrenceInterval = recurrenceInterval,
        recurrenceDaysOfWeekMask = recurrenceDaysOfWeekMask,
        recurrenceDayOfMonth = recurrenceDayOfMonth,
        recurrenceWeekOfMonth = recurrenceWeekOfMonth,
        recurrenceWeekday = recurrenceWeekday,
        recurrenceYearlyDates = recurrenceYearlyDates,
        recurrenceAnchorDate = recurrenceAnchorDate,
        recurrenceStartDate = recurrenceStartDate,
        recurrenceEndDate = recurrenceEndDate,
        type = HabitType.BOOLEAN
    )
}
