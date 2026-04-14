package com.example.habitpower.ui.daily

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyCheckInHabitInput(
    val habitId: Long,
    val name: String,
    val description: String,
    val goalIdentityStatement: String = "",
    val type: HabitType,
    val unit: String?,
    val targetValue: Double?,
    val textValue: String,
    val booleanValue: Boolean,
    /** For TIME habits: minutes-from-noon offset that will be persisted. */
    val numericTimeValue: Double = 0.0
) {
    val isCompleted: Boolean
        get() = when (type) {
            HabitType.BOOLEAN -> booleanValue
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> textValue.toDoubleOrNull() != null
            HabitType.TIME -> textValue.isNotBlank() // textValue stores "HH:MM AM/PM" display; numericValue used for save
            HabitType.TEXT -> textValue.isNotBlank()
            HabitType.ROUTINE -> booleanValue
        }

    val targetMet: Boolean?
        get() = when (type) {
            HabitType.BOOLEAN -> if (booleanValue) true else false
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> {
                val current = textValue.toDoubleOrNull()
                if (current == null || targetValue == null) null else current >= targetValue
            }
            HabitType.TIME -> null // evaluated after save via minutesFromNoon
            HabitType.TEXT -> null
            HabitType.ROUTINE -> if (booleanValue) true else false
        }
}

data class DailyCheckInUiState(
    val date: LocalDate,
    val userId: Long? = null,
    val userName: String? = null,
    val habits: List<DailyCheckInHabitInput> = emptyList(),
    val isLoading: Boolean = false
) {
    val completedCount: Int
        get() = habits.count { it.isCompleted }

    val totalCount: Int
        get() = habits.size
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DailyCheckInViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {
    private val maxBackfillDays = 3L
    private val requestedUserId = (
        savedStateHandle.get<String>("userId")?.toLongOrNull()
            ?: savedStateHandle.get<Long>("userId")
        )
        ?.takeIf { it >= 0L }
    private val date = (
        savedStateHandle.get<String>("date")
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        ).coerceIn(LocalDate.now().minusDays(maxBackfillDays), LocalDate.now())
    private val overrideInputs = MutableStateFlow<Map<Long, DailyCheckInHabitInput>>(emptyMap())

    private val selectedUser = if (requestedUserId != null) {
        repository.getAllUsers().map { users -> users.firstOrNull { it.id == requestedUserId } }
    } else {
        repository.getResolvedActiveUser()
    }

    private val sourceHabits = selectedUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            repository.getDailyHabitItems(user.id, date)
        }
    }

    val uiState: StateFlow<DailyCheckInUiState> = combine(selectedUser, sourceHabits, overrideInputs) { user, habits, overrides ->
        val mergedHabits = habits.map { item ->
            overrides[item.habitId] ?: item.toInput()
        }
        DailyCheckInUiState(
            date = date,
            userId = user?.id,
            userName = user?.name,
            habits = mergedHabits,
            isLoading = user == null && requestedUserId == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyCheckInUiState(date = date, isLoading = true)
    )

    fun updateTextValue(habitId: Long, value: String) {
        val habit = uiState.value.habits.firstOrNull { it.habitId == habitId } ?: return
        overrideInputs.value = overrideInputs.value + (habitId to habit.copy(textValue = value))
    }

    fun updateBooleanValue(habitId: Long, value: Boolean) {
        val habit = uiState.value.habits.firstOrNull { it.habitId == habitId } ?: return
        overrideInputs.value = overrideInputs.value + (habitId to habit.copy(booleanValue = value))
    }

    fun updateTimeValue(habitId: Long, displayText: String, minutesFromNoon: Double) {
        val habit = uiState.value.habits.firstOrNull { it.habitId == habitId } ?: return
        overrideInputs.value = overrideInputs.value + (
            habitId to habit.copy(
                textValue = displayText,
                numericTimeValue = minutesFromNoon
            )
            )
    }

    fun saveDailyCheckIn(onSaved: () -> Unit) {
        val state = uiState.value
        val userId = state.userId ?: return

        viewModelScope.launch {
            state.habits.forEach { habit ->
                when (habit.type) {
                    HabitType.BOOLEAN -> repository.saveDailyHabitEntry(
                        userId = userId,
                        date = date,
                        habitId = habit.habitId,
                        type = habit.type,
                        booleanValue = habit.booleanValue
                    )

                    HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> repository.saveDailyHabitEntry(
                        userId = userId,
                        date = date,
                        habitId = habit.habitId,
                        type = habit.type,
                        numericValue = habit.textValue.toDoubleOrNull()
                    )

                    HabitType.TIME -> repository.saveDailyHabitEntry(
                        userId = userId,
                        date = date,
                        habitId = habit.habitId,
                        type = habit.type,
                        numericValue = habit.numericTimeValue
                    )

                    HabitType.TEXT -> repository.saveDailyHabitEntry(
                        userId = userId,
                        date = date,
                        habitId = habit.habitId,
                        type = habit.type,
                        textValue = habit.textValue
                    )

                    HabitType.ROUTINE -> repository.saveDailyHabitEntry(
                        userId = userId,
                        date = date,
                        habitId = habit.habitId,
                        type = habit.type,
                        booleanValue = habit.booleanValue
                    )
                }
            }
            overrideInputs.value = emptyMap()
            // Award XP, update streak and badges based on what was just saved
            try { gamificationRepository.onDayCheckedIn(userId, date) } catch (_: Exception) {}
            onSaved()
        }
    }

    private fun DailyHabitItem.toInput(): DailyCheckInHabitInput {
        return DailyCheckInHabitInput(
            habitId = habitId,
            name = name,
            description = description,
            goalIdentityStatement = goalIdentityStatement,
            type = type,
            unit = unit,
            targetValue = targetValue,
            textValue = when (type) {
                HabitType.BOOLEAN -> ""
                HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> entryNumericValue?.toInt()?.toString() ?: ""
                HabitType.NUMBER, HabitType.DURATION -> entryNumericValue?.stripTrailingZero() ?: ""
                HabitType.TIME -> {
                    // Convert stored minutesFromNoon back to displayable "HH:MM AM/PM"
                    if (entryNumericValue == null) {
                        ""
                    } else {
                        val totalMins = ((entryNumericValue + 12 * 60) % (24 * 60)).toInt()
                        val h = totalMins / 60
                        val m = totalMins % 60
                        val amPm = if (h >= 12) "PM" else "AM"
                        val h12 = if (h % 12 == 0) 12 else h % 12
                        "%02d:%02d %s".format(h12, m, amPm)
                    }
                }
                HabitType.TEXT -> entryTextValue.orEmpty()
                HabitType.ROUTINE -> ""
            },
            numericTimeValue = entryNumericValue ?: 0.0,
            booleanValue = entryBooleanValue == true
        )
    }

    private fun Double.stripTrailingZero(): String {
        val longValue = toLong()
        return if (longValue.toDouble() == this) {
            longValue.toString()
        } else {
            toString()
        }
    }
}
