package com.example.habitpower.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitHealthState(val label: String, val description: String, val minRatio: Float) {
    THRIVING("Thriving", "80 %+ completion — this habit has become yours", 0.80f),
    STEADY("Steady", "50–79 % — consistent but room to lock in", 0.50f),
    STRUGGLING("Struggling", "20–49 % — needs attention or a smaller commitment", 0.20f),
    STALE("Stale", "Under 20 % — worth reviewing honestly", 0.0f)
}

data class HabitWithHealth(
    val habit: HabitDefinition,
    val healthState: HabitHealthState,
    val completionRatio: Float,
    val completedDays: Int,
    val scheduledDays: Int
)

data class HabitInventoryUiState(
    val isLoading: Boolean = true,
    val habitsByHealth: Map<HabitHealthState, List<HabitWithHealth>> = emptyMap(),
    val overCommitted: Boolean = false,
    val totalActive: Int = 0,
    val overallRatio: Float = 0f
)

class HabitInventoryViewModel(
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitInventoryUiState())
    val uiState: StateFlow<HabitInventoryUiState> = _uiState.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = repository.getResolvedActiveUser().first() ?: run {
                _uiState.value = HabitInventoryUiState(isLoading = false)
                return@launch
            }
            val habits = repository.getAssignedHabitsForUser(user.id).first()
                .filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }

            val habitWithHealthList = habits.map { habit ->
                val history = repository.getHabitHistoryDays(user.id, habit.id, 30)
                val scheduledDays = history.count { (date, _) -> HabitRecurrenceCalculator.isScheduledOn(date, habit) }
                val completedDays = history.count { (date, entry) ->
                    HabitRecurrenceCalculator.isScheduledOn(date, habit) && isEntryCompleted(habit.type, entry)
                }
                val ratio = if (scheduledDays > 0) completedDays.toFloat() / scheduledDays else 0f
                val healthState = when {
                    ratio >= HabitHealthState.THRIVING.minRatio -> HabitHealthState.THRIVING
                    ratio >= HabitHealthState.STEADY.minRatio -> HabitHealthState.STEADY
                    ratio >= HabitHealthState.STRUGGLING.minRatio -> HabitHealthState.STRUGGLING
                    else -> HabitHealthState.STALE
                }
                HabitWithHealth(habit, healthState, ratio, completedDays, scheduledDays)
            }

            val overallCompleted = habitWithHealthList.sumOf { it.completedDays }
            val overallScheduled = habitWithHealthList.sumOf { it.scheduledDays }
            val overallRatio = if (overallScheduled > 0) overallCompleted.toFloat() / overallScheduled else 0f
            val overCommitted = habits.size >= 7 && overallRatio < 0.5f

            val byHealth = HabitHealthState.entries
                .associateWith { state -> habitWithHealthList.filter { it.healthState == state } }
                .filter { (_, list) -> list.isNotEmpty() }

            _uiState.value = HabitInventoryUiState(
                isLoading = false,
                habitsByHealth = byHealth,
                overCommitted = overCommitted,
                totalActive = habits.size,
                overallRatio = overallRatio
            )
        }
    }

    fun retireHabit(habit: HabitDefinition) {
        viewModelScope.launch {
            repository.setHabitLifecycle(habit, HabitLifecycleStatus.RETIRED)
            loadInventory()
        }
    }

    fun pauseHabit(habit: HabitDefinition) {
        viewModelScope.launch {
            repository.setHabitLifecycle(habit, HabitLifecycleStatus.PAUSED)
            loadInventory()
        }
    }

    private fun isEntryCompleted(type: HabitType, entry: DailyHabitEntry?): Boolean {
        if (entry == null) return false
        return when (type) {
            HabitType.BOOLEAN, HabitType.ROUTINE -> entry.booleanValue == true
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT,
            HabitType.POMODORO, HabitType.TIMER, HabitType.TIME -> entry.numericValue != null
            HabitType.TEXT -> !entry.textValue.isNullOrBlank()
        }
    }

}
