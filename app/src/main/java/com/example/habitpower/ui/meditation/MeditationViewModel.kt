package com.example.habitpower.ui.meditation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MeditationViewModel(
    private val repository: HabitPowerRepository
) : ViewModel() {

    val habitsForLinking: StateFlow<List<HabitDefinition>> = repository.getAllHabits()
        .map { habits -> habits.filter { it.type != HabitType.TEXT && it.type != HabitType.ROUTINE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logSession(presetName: String, durationSeconds: Long) {
        viewModelScope.launch {
            val user = repository.getResolvedActiveUser().first() ?: return@launch
            repository.logMeditationSession(user.id, presetName, durationSeconds)
        }
    }

    fun linkSessionToHabit(habitId: Long) {
        viewModelScope.launch {
            val user = repository.getResolvedActiveUser().first() ?: return@launch
            val habit = habitsForLinking.value.firstOrNull { it.id == habitId } ?: return@launch
            repository.saveDailyHabitEntry(
                userId = user.id,
                date = LocalDate.now(),
                habitId = habitId,
                type = habit.type,
                booleanValue = if (habit.type == HabitType.BOOLEAN) true else null,
                numericValue = when (habit.type) {
                    HabitType.COUNT, HabitType.DURATION, HabitType.TIMER, HabitType.NUMBER -> habit.targetValue ?: 1.0
                    else -> null
                }
            )
        }
    }
}
