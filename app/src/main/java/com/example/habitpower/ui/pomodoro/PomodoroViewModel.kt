package com.example.habitpower.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.util.NotificationSoundOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PomodoroState(
    /** Combined list of POMODORO and TIMER habits for the active user today. */
    val pomodoroHabits: List<DailyHabitItem> = emptyList(),
    val selectedHabit: DailyHabitItem? = null,
    val targetEndTime: Long? = null,
    val isRunning: Boolean = false,
    val remainingSeconds: Long = 25 * 60,
    val showCelebration: Boolean = false,
    val isLoading: Boolean = false,
    val userId: Long? = null,
    /** Non-zero when a sound should fire; consumed by UI via LaunchedEffect(soundTrigger). */
    val soundTrigger: Long = 0L,
    val soundEnabled: Boolean = true,
    val selectedSoundId: String = NotificationSoundOption.SHORT_BEEP.id
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PomodoroViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val POMODORO_DURATION_SECONDS = 25 * 60L
    private val requestedHabitId: Long? = savedStateHandle.get<String>("habitId")?.toLongOrNull()?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(PomodoroState())
    val uiState: StateFlow<PomodoroState> = _uiState.asStateFlow()

    init {
        // Load focus habits (POMODORO + TIMER) for the active user
        viewModelScope.launch {
            repository.getResolvedActiveUser().flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    _uiState.update { it.copy(userId = user.id) }
                    repository.getFocusHabitItems(user.id, LocalDate.now())
                }
            }.collect { habits ->
                val focusHabits = habits.filter {
                    it.type == HabitType.POMODORO || it.type == HabitType.TIMER
                }
                val currentSelectedId = requestedHabitId ?: _uiState.value.selectedHabit?.habitId
                val newSelected = focusHabits.find { it.habitId == currentSelectedId }
                    ?: focusHabits.firstOrNull()
                _uiState.update { it.copy(pomodoroHabits = focusHabits, selectedHabit = newSelected) }
            }
        }

        // Load sound preferences
        viewModelScope.launch {
            combine(
                prefsRepository.soundEnabled,
                prefsRepository.notificationSoundId
            ) { enabled, soundId -> enabled to soundId }
                .collect { (enabled, soundId) ->
                    _uiState.update { it.copy(soundEnabled = enabled, selectedSoundId = soundId) }
                }
        }

        // Countdown ticker
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.isRunning && state.targetEndTime != null) {
                    val now = System.currentTimeMillis()
                    val remaining = (state.targetEndTime - now) / 1000L
                    if (remaining <= 0) {
                        completeSession()
                    } else {
                        _uiState.update { it.copy(remainingSeconds = remaining) }
                    }
                }
            }
        }
    }

    // ── Duration helpers ────────────────────────────────────────────────────────

    /** Returns the session duration in seconds for the given habit. */
    private fun sessionDurationSeconds(habit: DailyHabitItem): Long {
        return if (habit.type == HabitType.TIMER) {
            val minutes = habit.targetValue?.toLong() ?: 25L
            minutes.coerceIn(1L, 1440L) * 60L
        } else {
            POMODORO_DURATION_SECONDS
        }
    }

    // ── Public actions ──────────────────────────────────────────────────────────

    fun selectHabit(habit: DailyHabitItem) {
        if (!_uiState.value.isRunning) {
            _uiState.update {
                it.copy(
                    selectedHabit = habit,
                    remainingSeconds = sessionDurationSeconds(habit)
                )
            }
        }
    }

    fun startSession() {
        val habit = _uiState.value.selectedHabit ?: return
        val duration = sessionDurationSeconds(habit)
        val targetTime = System.currentTimeMillis() + duration * 1000L
        _uiState.update {
            it.copy(
                isRunning = true,
                targetEndTime = targetTime,
                showCelebration = false,
                remainingSeconds = duration
            )
        }
    }

    fun addMinute() {
        val current = _uiState.value.targetEndTime ?: return
        _uiState.update { it.copy(targetEndTime = current + 60_000L) }
    }

    fun finishSessionEarly() {
        completeSession()
    }

    fun cancelSession() {
        val habit = _uiState.value.selectedHabit
        _uiState.update {
            it.copy(
                isRunning = false,
                targetEndTime = null,
                remainingSeconds = if (habit != null) sessionDurationSeconds(habit) else POMODORO_DURATION_SECONDS
            )
        }
    }

    fun toggleSound() {
        val newEnabled = !_uiState.value.soundEnabled
        _uiState.update { it.copy(soundEnabled = newEnabled) }
        viewModelScope.launch { prefsRepository.saveSoundEnabled(newEnabled) }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private fun completeSession() {
        val state = _uiState.value
        val habit = state.selectedHabit ?: return
        val userId = state.userId ?: return

        _uiState.update {
            it.copy(
                isRunning = false,
                targetEndTime = null,
                remainingSeconds = sessionDurationSeconds(habit),
                showCelebration = true,
                soundTrigger = if (it.soundEnabled) System.currentTimeMillis() else it.soundTrigger
            )
        }

        viewModelScope.launch {
            when (habit.type) {
                HabitType.TIMER -> {
                    // One cycle = done: save the target minutes value → numericValue >= targetValue = completed
                    val minutesDone = habit.targetValue ?: 1.0
                    repository.saveDailyHabitEntry(
                        userId = userId,
                        date = LocalDate.now(),
                        habitId = habit.habitId,
                        type = HabitType.TIMER,
                        numericValue = minutesDone
                    )
                }
                HabitType.POMODORO -> {
                    val currentCount = habit.entryNumericValue ?: 0.0
                    repository.saveDailyHabitEntry(
                        userId = userId,
                        date = LocalDate.now(),
                        habitId = habit.habitId,
                        type = HabitType.POMODORO,
                        numericValue = currentCount + 1.0
                    )
                }
                else -> {}
            }
        }
    }

    fun checkResumeState() {
        val state = _uiState.value
        if (state.isRunning && state.targetEndTime != null) {
            val now = System.currentTimeMillis()
            if (now >= state.targetEndTime) {
                completeSession()
            }
        }
    }
}
