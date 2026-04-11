package com.example.habitpower.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.TargetOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminHabitsViewModel(private val repository: HabitPowerRepository) : ViewModel() {
    private val _createSuccessTick = MutableStateFlow(0L)
    val createSuccessTick: StateFlow<Long> = _createSuccessTick

    var name by mutableStateOf("")
        private set
    var goalIdentityStatement by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var selectedType by mutableStateOf(HabitType.BOOLEAN)
        private set
    var unit by mutableStateOf("")
        private set
    var targetValue by mutableStateOf("")
        private set

    /** For TIME habits: the hour component of the target bedtime (24h). */
    var targetHour by mutableStateOf(22)
        private set

    /** For TIME habits: the minute component of the target bedtime. */
    var targetMinute by mutableStateOf(0)
        private set
    var commitmentHour by mutableStateOf(7)
        private set
    var commitmentMinute by mutableStateOf(0)
        private set
    var commitmentLocation by mutableStateOf("")
        private set
    var preReminderEnabled by mutableStateOf(false)
        private set
    var preReminderMinutes by mutableStateOf("15")
        private set
    var selectedOperator by mutableStateOf(TargetOperator.GREATER_THAN_OR_EQUAL)
        private set
    var selectedLifeAreaId by mutableStateOf<Long?>(null)
        private set

    val habits: StateFlow<List<HabitDefinition>> = repository.getAllHabits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val lifeAreas: StateFlow<List<LifeArea>> = repository.getAllLifeAreas().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun updateName(value: String) { name = value }
    fun updateGoalIdentityStatement(value: String) { goalIdentityStatement = value }
    fun updateDescription(value: String) { description = value }

    fun updateType(type: HabitType) {
        selectedType = type
        if (type == HabitType.BOOLEAN || type == HabitType.TEXT) {
            unit = ""
            targetValue = ""
        }
        if (type == HabitType.TIMER) {
            unit = "minutes"
            selectedOperator = TargetOperator.GREATER_THAN_OR_EQUAL
        }
        // Default operator for time: LESS_THAN_OR_EQUAL (sleep BEFORE target)
        if (type == HabitType.TIME) {
            selectedOperator = TargetOperator.LESS_THAN_OR_EQUAL
        }
    }

    fun updateUnit(value: String) { unit = value }
    fun updateTargetValue(value: String) { targetValue = value }
    fun updateTargetTime(hour: Int, minute: Int) {
        targetHour = hour
        targetMinute = minute
    }
    fun updateCommitmentTime(hour: Int, minute: Int) {
        commitmentHour = hour
        commitmentMinute = minute
    }
    fun updateCommitmentLocation(value: String) { commitmentLocation = value }
    fun updatePreReminderEnabled(enabled: Boolean) { preReminderEnabled = enabled }
    fun updatePreReminderMinutes(value: String) {
        preReminderMinutes = value.filter { it.isDigit() }
    }
    fun updateOperator(op: TargetOperator) { selectedOperator = op }
    fun updateSelectedLifeArea(id: Long?) { selectedLifeAreaId = id }

    fun createHabit() {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        val trimmedGoalIdentity = goalIdentityStatement.trim()
        if (trimmedGoalIdentity.isBlank()) return
        val trimmedDescription = description.trim()
        if (trimmedDescription.isBlank()) return
        val trimmedLocation = commitmentLocation.trim()
        if (trimmedLocation.isBlank()) return

        viewModelScope.launch {
            val resolvedTarget: Double? = when (selectedType) {
                HabitType.BOOLEAN, HabitType.TEXT -> null
                HabitType.TIME -> minutesFromNoon(targetHour, targetMinute)
                HabitType.TIMER -> targetValue.toDoubleOrNull()?.coerceIn(1.0, 1440.0)
                else -> targetValue.toDoubleOrNull()
            }
            val resolvedReminderMinutes = if (preReminderEnabled) {
                preReminderMinutes.toIntOrNull()?.coerceIn(1, 1440)
            } else {
                null
            }
            if (preReminderEnabled && resolvedReminderMinutes == null) return@launch
            repository.createHabit(
                name = trimmedName,
                goalIdentityStatement = trimmedGoalIdentity,
                description = trimmedDescription,
                type = selectedType,
                unit = when (selectedType) {
                    HabitType.BOOLEAN, HabitType.TEXT -> null
                    HabitType.TIMER -> "minutes"
                    else -> unit
                },
                targetValue = resolvedTarget,
                operator = selectedOperator,
                lifeAreaId = selectedLifeAreaId,
                showInDailyCheckIn = selectedType != HabitType.TIMER,
                commitmentTime = "%02d:%02d".format(commitmentHour, commitmentMinute),
                commitmentLocation = trimmedLocation,
                preReminderMinutes = resolvedReminderMinutes
            )
            name = ""
            goalIdentityStatement = ""
            description = ""
            selectedType = HabitType.BOOLEAN
            unit = ""
            targetValue = ""
            targetHour = 22
            targetMinute = 0
            commitmentHour = 7
            commitmentMinute = 0
            commitmentLocation = ""
            preReminderEnabled = false
            preReminderMinutes = "15"
            selectedOperator = TargetOperator.GREATER_THAN_OR_EQUAL
            selectedLifeAreaId = null
            _createSuccessTick.value += 1
        }
    }

    fun updateHabit(habit: HabitDefinition, newName: String, newDescription: String, newTarget: String?, newOp: TargetOperator) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val resolvedTarget = when (habit.type) {
            HabitType.BOOLEAN, HabitType.TEXT -> null
            HabitType.TIME -> newTarget?.toDoubleOrNull()
            else -> newTarget?.toDoubleOrNull()
        }
        viewModelScope.launch {
            repository.updateHabit(
                habit.copy(
                    name = trimmed,
                    description = newDescription.trim(),
                    targetValue = resolvedTarget,
                    operator = newOp
                )
            )
        }
    }

    fun deleteHabit(habit: HabitDefinition) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    /**
     * Converts 24h clock time to monotonic "minutes from noon" so that
     * early-morning times (after midnight) are always greater than evening times.
     * Noon = 0, 10 PM = 600, Midnight = 720, 6 AM = 1080.
     */
    private fun minutesFromNoon(hour: Int, minute: Int): Double {
        val minutesSinceMidnight = hour * 60 + minute
        return ((minutesSinceMidnight - 12 * 60 + 24 * 60) % (24 * 60)).toDouble()
    }
}
