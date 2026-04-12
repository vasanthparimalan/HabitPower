package com.example.habitpower.reminder

import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HabitRecurrenceCalculatorTest {

    @Test
    fun weeklySelectedDays_matchesOnlyChosenWeekdays() {
        val habit = testHabit(
            recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
            recurrenceDaysOfWeekMask = (1 shl 1) or (1 shl 3) or (1 shl 5)
        )

        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 13), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 15), habit))
        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 17), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 14), habit))
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
            recurrenceWeekday = 2,
            recurrenceStartDate = LocalDate.of(2026, 1, 1)
        )

        assertTrue(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 28), habit))
        assertFalse(HabitRecurrenceCalculator.isScheduledOn(LocalDate.of(2026, 4, 21), habit))
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
    ): HabitDefinition {
        return HabitDefinition(
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
}
