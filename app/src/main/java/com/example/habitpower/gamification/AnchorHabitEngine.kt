package com.example.habitpower.gamification

import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object AnchorHabitEngine {

    data class AnchorHabit(
        val habitId: Long,
        val habitName: String,
        val multiplier: Float,
        val doneDays: Int
    )

    fun compute(
        habits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        start: LocalDate,
        end: LocalDate,
        minMultiplier: Float = 1.5f,
        minDoneDays: Int = 7
    ): AnchorHabit? {
        val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
        if (totalDays < 21) return null

        val activeHabits = habits.filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }
        if (activeHabits.size < 2) return null

        return activeHabits
            .mapNotNull { candidate ->
                computeMultiplier(candidate, activeHabits, entriesByDate, start, end, minDoneDays)
            }
            .filter { it.multiplier >= minMultiplier }
            .maxByOrNull { it.multiplier }
    }

    private fun computeMultiplier(
        anchor: HabitDefinition,
        allHabits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        start: LocalDate,
        end: LocalDate,
        minDoneDays: Int
    ): AnchorHabit? {
        val others = allHabits.filter { it.id != anchor.id }
        if (others.isEmpty()) return null

        val doneDayRates = mutableListOf<Float>()
        val missedDayRates = mutableListOf<Float>()

        var date = start
        while (!date.isAfter(end)) {
            if (!HabitRecurrenceCalculator.isScheduledOn(date, anchor)) {
                date = date.plusDays(1)
                continue
            }
            val dayEntryIds = entriesByDate[date].orEmpty().map { it.habitId }.toSet()
            val anchorDone = anchor.id in dayEntryIds

            val otherScheduled = others.filter { HabitRecurrenceCalculator.isScheduledOn(date, it) }
            if (otherScheduled.isNotEmpty()) {
                val rate = otherScheduled.count { it.id in dayEntryIds }.toFloat() / otherScheduled.size
                if (anchorDone) doneDayRates.add(rate) else missedDayRates.add(rate)
            }
            date = date.plusDays(1)
        }

        if (doneDayRates.size < minDoneDays) return null
        val meanDone = doneDayRates.average().toFloat()
        val meanMissed = if (missedDayRates.isEmpty()) 0f else missedDayRates.average().toFloat()
        if (meanMissed < 0.01f) return null

        return AnchorHabit(
            habitId = anchor.id,
            habitName = anchor.name,
            multiplier = meanDone / meanMissed,
            doneDays = doneDayRates.size
        )
    }
}
