package com.example.habitpower.gamification

import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import java.time.LocalDate

object IdentitySentenceEngine {

    data class IdentitySentence(
        val sentence: String,
        val habitId: Long,
        val completionRate: Float
    )

    fun compute(
        habits: List<HabitDefinition>,
        entriesByDate: Map<LocalDate, List<DailyHabitEntry>>,
        start: LocalDate,
        end: LocalDate,
        minScheduledDays: Int = 14,
        minCompletionRate: Float = 0.70f,
        maxSentences: Int = 3
    ): List<IdentitySentence> {
        val activeHabits = habits.filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }

        return activeHabits
            .mapNotNull { habit ->
                var scheduled = 0
                var completed = 0
                var date = start
                while (!date.isAfter(end)) {
                    if (HabitRecurrenceCalculator.isScheduledOn(date, habit)) {
                        scheduled++
                        if (entriesByDate[date].orEmpty().any { it.habitId == habit.id }) {
                            completed++
                        }
                    }
                    date = date.plusDays(1)
                }
                if (scheduled < minScheduledDays) return@mapNotNull null
                val rate = completed.toFloat() / scheduled
                if (rate < minCompletionRate) return@mapNotNull null
                IdentitySentence(
                    sentence = buildSentence(habit),
                    habitId = habit.id,
                    completionRate = rate
                )
            }
            .sortedByDescending { it.completionRate }
            .take(maxSentences)
    }

    private fun buildSentence(habit: HabitDefinition): String {
        if (habit.goalIdentityStatement.isNotBlank()) return habit.goalIdentityStatement
        return "You are someone who ${inferVerbPhrase(habit.name, habit.type)}."
    }

    private fun inferVerbPhrase(name: String, type: HabitType): String {
        val lower = name.lowercase()
        return when {
            lower.contains("read") -> "reads every day"
            lower.contains("meditat") -> "meditates daily"
            lower.contains("exercise") || lower.contains("workout") || lower.contains("gym") -> "moves their body every day"
            lower.contains("run") || lower.contains("jog") -> "runs regularly"
            lower.contains("walk") -> "walks daily"
            lower.contains("journal") || lower.contains("diary") -> "journals regularly"
            lower.contains("writ") -> "writes consistently"
            lower.contains("sleep") || lower.contains("bed") -> "honors their sleep"
            lower.contains("water") || lower.contains("hydrat") -> "stays hydrated"
            lower.contains("plan") -> "plans their day intentionally"
            lower.contains("pray") || lower.contains("namaz") || lower.contains("puja") -> "keeps a daily prayer practice"
            lower.contains("yoga") -> "practices yoga"
            lower.contains("gratitude") || lower.contains("grateful") -> "practices gratitude"
            lower.contains("vitam") || lower.contains("supplement") -> "takes care of their health"
            lower.contains("stretch") -> "stretches every day"
            lower.contains("breath") -> "practices breathwork"
            lower.contains("cold") && lower.contains("shower") -> "takes cold showers"
            lower.contains("fast") -> "practices intentional fasting"
            lower.contains("learn") || lower.contains("study") -> "keeps learning every day"
            lower.contains("code") || lower.contains("program") -> "writes code consistently"
            type == HabitType.POMODORO -> "does deep focused work"
            type == HabitType.TIMER -> "shows up for their practice every day"
            else -> "shows up for their ${name.lowercase()} practice"
        }
    }
}
