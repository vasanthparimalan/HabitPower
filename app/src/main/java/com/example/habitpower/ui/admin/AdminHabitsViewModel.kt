package com.example.habitpower.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.TargetOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    var selectedRoutineId by mutableStateOf<Long?>(null)
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
    var selectedRecurrenceType by mutableStateOf(HabitRecurrenceType.DAILY)
        private set
    var selectedWeekdaysMask by mutableStateOf(0)
        private set
    var everyNDaysInterval by mutableStateOf("14")
        private set
    var monthlyDayOfMonth by mutableStateOf("1")
        private set
    var monthlyNthWeek by mutableStateOf("1")
        private set
    var monthlyNthWeekday by mutableStateOf("1")
        private set
    var yearlyDatesCsv by mutableStateOf("01-01")
        private set
    var recurrenceStartDateText by mutableStateOf(LocalDate.now().toString())
        private set
    var recurrenceEndDateText by mutableStateOf("")
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

    val routines: StateFlow<List<Routine>> = repository.getAllRoutines().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun updateName(value: String) { name = value }
    fun updateGoalIdentityStatement(value: String) { goalIdentityStatement = value }
    fun updateDescription(value: String) { description = value }
    fun updateSelectedRoutine(routineId: Long?) { selectedRoutineId = routineId }

    fun updateType(type: HabitType) {
        selectedType = type
        if (type == HabitType.BOOLEAN || type == HabitType.TEXT || type == HabitType.ROUTINE) {
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
        if (type != HabitType.ROUTINE) {
            selectedRoutineId = null
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
    fun updateRecurrenceType(type: HabitRecurrenceType) { selectedRecurrenceType = type }
    fun toggleWeekday(dayOfWeekValue: Int) {
        val bit = 1 shl dayOfWeekValue
        selectedWeekdaysMask = if ((selectedWeekdaysMask and bit) != 0) {
            selectedWeekdaysMask and bit.inv()
        } else {
            selectedWeekdaysMask or bit
        }
    }
    fun updateEveryNDaysInterval(value: String) { everyNDaysInterval = value.filter { it.isDigit() } }
    fun updateMonthlyDayOfMonth(value: String) { monthlyDayOfMonth = value.filter { it.isDigit() } }
    fun updateMonthlyNthWeek(value: String) { monthlyNthWeek = value.filter { it.isDigit() || it == '-' } }
    fun updateMonthlyNthWeekday(value: String) { monthlyNthWeekday = value.filter { it.isDigit() } }
    fun updateYearlyDatesCsv(value: String) {
        yearlyDatesCsv = value
            .uppercase()
            .filter { it.isDigit() || it == '-' || it == ',' || it == ' ' }
    }
    fun updateRecurrenceStartDateText(value: String) { recurrenceStartDateText = value.trim() }
    fun updateRecurrenceEndDateText(value: String) { recurrenceEndDateText = value.trim() }

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
                HabitType.BOOLEAN, HabitType.TEXT, HabitType.ROUTINE -> null
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
            val startDate = recurrenceStartDateText.toLocalDateOrNull()
            if (startDate == null) return@launch
            val endDate = recurrenceEndDateText.toLocalDateOrNull()
            if (recurrenceEndDateText.isNotBlank() && endDate == null) return@launch

            val recurrenceInterval = when (selectedRecurrenceType) {
                HabitRecurrenceType.EVERY_N_DAYS -> everyNDaysInterval.toIntOrNull()?.coerceAtLeast(1) ?: return@launch
                else -> 1
            }
            val recurrenceDayOfMonth = when (selectedRecurrenceType) {
                HabitRecurrenceType.MONTHLY_BY_DATE -> monthlyDayOfMonth.toIntOrNull()?.coerceIn(1, 31) ?: return@launch
                else -> null
            }
            val recurrenceWeekOfMonth = when (selectedRecurrenceType) {
                HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                    val week = monthlyNthWeek.toIntOrNull() ?: return@launch
                    if (week in 1..5 || week == -1) week else return@launch
                }
                else -> null
            }
            val recurrenceWeekday = when (selectedRecurrenceType) {
                HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> monthlyNthWeekday.toIntOrNull()?.coerceIn(1, 7) ?: return@launch
                else -> null
            }
            val yearlyDates = when (selectedRecurrenceType) {
                HabitRecurrenceType.YEARLY_BY_DATE,
                HabitRecurrenceType.YEARLY_MULTI_DATE -> {
                    val normalized = yearlyDatesCsv
                        .split(',')
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.matches(Regex("^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) }
                        .distinct()
                        .toList()
                    if (normalized.isEmpty()) return@launch
                    normalized.joinToString(",")
                }
                else -> ""
            }
            val recurrenceMask = when (selectedRecurrenceType) {
                HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> if (selectedWeekdaysMask == 0) return@launch else selectedWeekdaysMask
                else -> 0
            }

            repository.createHabit(
                name = trimmedName,
                goalIdentityStatement = trimmedGoalIdentity,
                description = trimmedDescription,
                type = selectedType,
                routineId = selectedRoutineId,
                unit = when (selectedType) {
                    HabitType.BOOLEAN, HabitType.TEXT, HabitType.ROUTINE -> null
                    HabitType.TIMER -> "minutes"
                    else -> unit
                },
                targetValue = resolvedTarget,
                operator = selectedOperator,
                lifeAreaId = selectedLifeAreaId,
                showInDailyCheckIn = selectedType != HabitType.TIMER,
                commitmentTime = "%02d:%02d".format(commitmentHour, commitmentMinute),
                commitmentLocation = trimmedLocation,
                preReminderMinutes = resolvedReminderMinutes,
                recurrenceType = selectedRecurrenceType,
                recurrenceInterval = recurrenceInterval,
                recurrenceDaysOfWeekMask = recurrenceMask,
                recurrenceDayOfMonth = recurrenceDayOfMonth,
                recurrenceWeekOfMonth = recurrenceWeekOfMonth,
                recurrenceWeekday = recurrenceWeekday,
                recurrenceYearlyDates = yearlyDates,
                recurrenceAnchorDate = startDate,
                recurrenceStartDate = startDate,
                recurrenceEndDate = endDate
            )
            name = ""
            goalIdentityStatement = ""
            description = ""
            selectedType = HabitType.BOOLEAN
            unit = ""
            targetValue = ""
            selectedRoutineId = null
            targetHour = 22
            targetMinute = 0
            commitmentHour = 7
            commitmentMinute = 0
            commitmentLocation = ""
            preReminderEnabled = false
            preReminderMinutes = "15"
            selectedOperator = TargetOperator.GREATER_THAN_OR_EQUAL
            selectedLifeAreaId = null
            selectedRecurrenceType = HabitRecurrenceType.DAILY
            selectedWeekdaysMask = 0
            everyNDaysInterval = "14"
            monthlyDayOfMonth = "1"
            monthlyNthWeek = "1"
            monthlyNthWeekday = "1"
            yearlyDatesCsv = "01-01"
            recurrenceStartDateText = LocalDate.now().toString()
            recurrenceEndDateText = ""
            _createSuccessTick.value += 1
        }
    }

    fun updateHabit(habit: HabitDefinition, newName: String, newDescription: String, newTarget: String?, newOp: TargetOperator) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val resolvedTarget = when (habit.type) {
            HabitType.BOOLEAN, HabitType.TEXT, HabitType.ROUTINE -> null
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

    fun updateHabitWithRecurrence(
        habit: HabitDefinition,
        newName: String,
        newDescription: String,
        newTarget: String?,
        newOp: TargetOperator,
        newRoutineId: Long?,
        newLifeAreaId: Long?,
        recurrenceType: HabitRecurrenceType,
        recurrenceDaysOfWeekMask: Int,
        recurrenceIntervalText: String,
        recurrenceDayOfMonthText: String,
        recurrenceWeekOfMonthText: String,
        recurrenceWeekdayText: String,
        yearlyDatesCsv: String,
        startDateText: String,
        endDateText: String
    ) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        val resolvedTarget = when (habit.type) {
            HabitType.BOOLEAN, HabitType.TEXT, HabitType.ROUTINE -> null
            HabitType.TIME -> newTarget?.toDoubleOrNull()
            else -> newTarget?.toDoubleOrNull()
        }

        val startDate = startDateText.trim().toLocalDateOrNull() ?: return
        val endDate = endDateText.trim().takeIf { it.isNotBlank() }?.toLocalDateOrNull()
        if (endDateText.isNotBlank() && endDate == null) return

        val recurrenceInterval = when (recurrenceType) {
            HabitRecurrenceType.EVERY_N_DAYS -> recurrenceIntervalText.toIntOrNull()?.coerceAtLeast(1) ?: return
            else -> 1
        }

        val recurrenceDayOfMonth = when (recurrenceType) {
            HabitRecurrenceType.MONTHLY_BY_DATE -> recurrenceDayOfMonthText.toIntOrNull()?.coerceIn(1, 31) ?: return
            else -> null
        }

        val recurrenceWeekOfMonth = when (recurrenceType) {
            HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                val week = recurrenceWeekOfMonthText.toIntOrNull() ?: return
                if (week in 1..5 || week == -1) week else return
            }
            else -> null
        }

        val recurrenceWeekday = when (recurrenceType) {
            HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> recurrenceWeekdayText.toIntOrNull()?.coerceIn(1, 7) ?: return
            else -> null
        }

        val normalizedYearlyDates = when (recurrenceType) {
            HabitRecurrenceType.YEARLY_BY_DATE,
            HabitRecurrenceType.YEARLY_MULTI_DATE -> {
                val normalized = yearlyDatesCsv
                    .split(',')
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.matches(Regex("^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) }
                    .distinct()
                    .toList()
                if (normalized.isEmpty()) return
                normalized.joinToString(",")
            }
            else -> ""
        }

        val normalizedWeekMask = when (recurrenceType) {
            HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> if (recurrenceDaysOfWeekMask == 0) return else recurrenceDaysOfWeekMask
            else -> 0
        }

        viewModelScope.launch {
            repository.updateHabit(
                habit.copy(
                    name = trimmed,
                    description = newDescription.trim(),
                    targetValue = resolvedTarget,
                    operator = newOp,
                    routineId = if (habit.type == HabitType.ROUTINE) newRoutineId else habit.routineId,
                    lifeAreaId = newLifeAreaId,
                    recurrenceType = recurrenceType,
                    recurrenceDaysOfWeekMask = normalizedWeekMask,
                    recurrenceInterval = recurrenceInterval,
                    recurrenceDayOfMonth = recurrenceDayOfMonth,
                    recurrenceWeekOfMonth = recurrenceWeekOfMonth,
                    recurrenceWeekday = recurrenceWeekday,
                    recurrenceYearlyDates = normalizedYearlyDates,
                    recurrenceAnchorDate = startDate,
                    recurrenceStartDate = startDate,
                    recurrenceEndDate = endDate
                )
            )
        }
    }

    fun setHabitLifecycle(habit: HabitDefinition, status: HabitLifecycleStatus) {
        viewModelScope.launch {
            repository.setHabitLifecycle(habit, status)
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

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
}
