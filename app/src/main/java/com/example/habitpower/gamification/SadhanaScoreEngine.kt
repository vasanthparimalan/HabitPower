package com.example.habitpower.gamification

import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.HabitType
import java.time.LocalDate

object SadhanaScoreEngine {

    data class Score(
        val total: Int,
        val habitComponent: Int,
        val sleepComponent: Int,
        val focusComponent: Int,
        val date: LocalDate,
        val hasSleepData: Boolean,
        val hasFocusData: Boolean
    )

    fun compute(habits: List<DailyHabitItem>, stat: DailyHealthStat?, date: LocalDate, meditationSessionsToday: Int = 0): Score {
        val trackable = habits.filter { it.type != HabitType.POMODORO && it.type != HabitType.TIMER }
        val completed = trackable.count { isCompleted(it) }
        val habitScore = if (trackable.isNotEmpty()) completed * 100 / trackable.size else 0

        val focusHabits = habits.filter { it.type == HabitType.POMODORO || it.type == HabitType.TIMER }
        val focusCompleted = focusHabits.count { isCompleted(it) }
        val meditationBonus = if (meditationSessionsToday > 0) 1 else 0
        val virtualFocusTotal = focusHabits.size + meditationBonus
        val virtualFocusCompleted = focusCompleted + meditationBonus
        val hasFocusData = virtualFocusTotal > 0
        val focusScore = if (hasFocusData) virtualFocusCompleted * 100 / virtualFocusTotal else 0

        val hasSleepData = stat != null && stat.sleepHours > 0f
        val sleepScore = if (hasSleepData) ((stat!!.sleepHours / 7f) * 100).toInt().coerceAtMost(100) else 0

        val total = when {
            hasSleepData && hasFocusData -> (habitScore * 0.50 + sleepScore * 0.25 + focusScore * 0.25).toInt()
            hasSleepData -> (habitScore * 0.65 + sleepScore * 0.35).toInt()
            hasFocusData -> (habitScore * 0.70 + focusScore * 0.30).toInt()
            else -> habitScore
        }

        return Score(
            total = total.coerceIn(0, 100),
            habitComponent = habitScore,
            sleepComponent = sleepScore,
            focusComponent = focusScore,
            date = date,
            hasSleepData = hasSleepData,
            hasFocusData = hasFocusData
        )
    }

    fun computeSimple(completedHabits: Int, totalHabits: Int, sleepHours: Float?): Int {
        val habitScore = if (totalHabits > 0) completedHabits * 100 / totalHabits else 0
        val hasSleep = sleepHours != null && sleepHours > 0f
        val sleepScore = if (hasSleep) ((sleepHours!! / 7f) * 100).toInt().coerceAtMost(100) else 0
        return if (hasSleep) (habitScore * 0.65 + sleepScore * 0.35).toInt().coerceIn(0, 100)
        else habitScore.coerceIn(0, 100)
    }

    private fun isCompleted(habit: DailyHabitItem): Boolean = when (habit.type) {
        HabitType.BOOLEAN, HabitType.ROUTINE -> habit.entryBooleanValue == true
        HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT,
        HabitType.POMODORO, HabitType.TIMER, HabitType.TIME -> habit.entryNumericValue != null
        HabitType.TEXT -> !habit.entryTextValue.isNullOrBlank()
    }
}
