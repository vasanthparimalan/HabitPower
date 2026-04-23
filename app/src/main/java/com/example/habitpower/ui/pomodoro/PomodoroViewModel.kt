package com.example.habitpower.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.PomodoroSettings
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.PomodoroSession
import com.example.habitpower.util.NotificationSoundOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

data class PomodoroState(
    val pomodoroHabits: List<DailyHabitItem> = emptyList(),
    val selectedHabit: DailyHabitItem? = null,
    val targetEndTime: Long? = null,
    val isRunning: Boolean = false,
    /** True while in an active session (between phases); false only when fully idle or cancelled. */
    val isInSession: Boolean = false,
    val remainingSeconds: Long = 25 * 60,
    val showCelebration: Boolean = false,
    val isLoading: Boolean = false,
    val userId: Long? = null,
    val soundTrigger: Long = 0L,
    val soundEnabled: Boolean = true,
    val selectedSoundId: String = NotificationSoundOption.SHORT_BEEP.id,

    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val cyclesCompleted: Int = 0,
    val isFreeSession: Boolean = false,

    val settings: PomodoroSettings = PomodoroSettings(),
    val showSettings: Boolean = false,

    val unlinkedSessions: List<PomodoroSession> = emptyList(),
    val showLinkDialog: Boolean = false,
    val sessionToLink: PomodoroSession? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PomodoroViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

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

        // Load pomodoro settings
        viewModelScope.launch {
            prefsRepository.pomodoroSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        // Observe today's unlinked free sessions
        viewModelScope.launch {
            repository.getResolvedActiveUser().flatMapLatest { user ->
                if (user == null) flowOf(emptyList())
                else repository.getUnlinkedSessionsForDate(user.id, LocalDate.now())
            }.collect { sessions ->
                _uiState.update { it.copy(unlinkedSessions = sessions) }
            }
        }

        // Cleanup unlinked sessions from previous days
        viewModelScope.launch {
            val user = repository.getResolvedActiveUser().firstOrNull()
            user?.let { repository.cleanupOldUnlinkedSessions(it.id) }
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
                        when (state.phase) {
                            PomodoroPhase.FOCUS -> completeFocusInterval()
                            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> completeBreak()
                        }
                    } else {
                        _uiState.update { it.copy(remainingSeconds = remaining) }
                    }
                }
            }
        }
    }

    // ── Duration helpers ─────────────────────────────────────────────────────

    private fun focusDurationSeconds(state: PomodoroState = _uiState.value): Long {
        val habit = state.selectedHabit
        return if (!state.isFreeSession && habit?.type == HabitType.TIMER) {
            val minutes = habit.targetValue?.toLong() ?: state.settings.focusMinutes.toLong()
            minutes.coerceIn(1L, 1440L) * 60L
        } else {
            state.settings.focusMinutes.toLong() * 60L
        }
    }

    private fun nextPhase(state: PomodoroState): PomodoroPhase {
        val nextCycles = state.cyclesCompleted + 1
        return if (nextCycles % state.settings.cyclesBeforeLongBreak == 0) {
            PomodoroPhase.LONG_BREAK
        } else {
            PomodoroPhase.SHORT_BREAK
        }
    }

    // ── Public actions ───────────────────────────────────────────────────────

    fun selectHabit(habit: DailyHabitItem) {
        if (!_uiState.value.isInSession) {
            val duration = focusDurationSeconds(_uiState.value.copy(selectedHabit = habit, isFreeSession = false))
            _uiState.update {
                it.copy(
                    selectedHabit = habit,
                    isFreeSession = false,
                    remainingSeconds = duration,
                    phase = PomodoroPhase.FOCUS,
                    cyclesCompleted = 0
                )
            }
        }
    }

    fun startHabitSession() {
        val habit = _uiState.value.selectedHabit ?: return
        val state = _uiState.value.copy(isFreeSession = false, selectedHabit = habit)
        val duration = focusDurationSeconds(state)
        val targetTime = System.currentTimeMillis() + duration * 1000L
        _uiState.update {
            it.copy(
                isRunning = true,
                isInSession = true,
                isFreeSession = false,
                targetEndTime = targetTime,
                showCelebration = false,
                remainingSeconds = duration,
                phase = PomodoroPhase.FOCUS,
                cyclesCompleted = 0
            )
        }
    }

    fun startFreeSession() {
        val state = _uiState.value.copy(isFreeSession = true, selectedHabit = null)
        val duration = focusDurationSeconds(state)
        val targetTime = System.currentTimeMillis() + duration * 1000L
        _uiState.update {
            it.copy(
                isRunning = true,
                isInSession = true,
                isFreeSession = true,
                selectedHabit = null,
                targetEndTime = targetTime,
                showCelebration = false,
                remainingSeconds = duration,
                phase = PomodoroPhase.FOCUS,
                cyclesCompleted = 0
            )
        }
    }

    /** User taps "Start Break" or "Start Round N" — begins the current paused phase. */
    fun startCurrentPhase() {
        val state = _uiState.value
        if (state.isInSession && !state.isRunning) {
            val targetTime = System.currentTimeMillis() + state.remainingSeconds * 1000L
            _uiState.update {
                it.copy(
                    isRunning = true,
                    targetEndTime = targetTime,
                    showCelebration = false
                )
            }
        }
    }

    fun addMinute() {
        val current = _uiState.value.targetEndTime ?: return
        _uiState.update { it.copy(targetEndTime = current + 60_000L) }
    }

    fun finishFocusEarly() {
        if (_uiState.value.phase == PomodoroPhase.FOCUS) completeFocusInterval()
    }

    fun skipBreak() {
        if (_uiState.value.phase == PomodoroPhase.SHORT_BREAK ||
            _uiState.value.phase == PomodoroPhase.LONG_BREAK) {
            pauseAtNextFocus()
        }
    }

    fun cancelFullSession() {
        val state = _uiState.value
        val focusDur = focusDurationSeconds(state.copy(phase = PomodoroPhase.FOCUS))
        _uiState.update {
            it.copy(
                isRunning = false,
                isInSession = false,
                targetEndTime = null,
                remainingSeconds = focusDur,
                phase = PomodoroPhase.FOCUS,
                cyclesCompleted = 0,
                showCelebration = false,
                sessionToLink = null
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

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun saveSettings(settings: PomodoroSettings) {
        _uiState.update {
            val focusDur = settings.focusMinutes.toLong() * 60L
            it.copy(
                settings = settings,
                showSettings = false,
                remainingSeconds = if (!it.isRunning && it.phase == PomodoroPhase.FOCUS) focusDur else it.remainingSeconds
            )
        }
        viewModelScope.launch { prefsRepository.savePomodoroSettings(settings) }
    }

    fun linkSession(session: PomodoroSession, habit: DailyHabitItem) {
        val userId = _uiState.value.userId ?: return
        viewModelScope.launch {
            repository.linkSessionToHabit(
                sessionId = session.id,
                habitId = habit.habitId,
                userId = userId,
                currentPomodoroCount = habit.entryNumericValue ?: 0.0
            )
        }
        _uiState.update { it.copy(showLinkDialog = false, sessionToLink = null) }
    }

    fun openLinkDialog(session: PomodoroSession) {
        _uiState.update { it.copy(showLinkDialog = true, sessionToLink = session) }
    }

    fun dismissLinkDialog() {
        _uiState.update { it.copy(showLinkDialog = false, sessionToLink = null) }
    }

    fun discardSession(session: PomodoroSession) {
        viewModelScope.launch { repository.discardSession(session) }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun completeFocusInterval() {
        val state = _uiState.value
        val userId = state.userId ?: return
        val newCycles = state.cyclesCompleted + 1
        val phase = nextPhase(state)
        val breakDur = if (newCycles % state.settings.cyclesBeforeLongBreak == 0) {
            state.settings.longBreakMinutes.toLong() * 60L
        } else {
            state.settings.shortBreakMinutes.toLong() * 60L
        }

        // Pause at the start of the break — user must tap "Start Break" to begin
        _uiState.update {
            it.copy(
                isRunning = false,
                isInSession = true,
                targetEndTime = null,
                phase = phase,
                cyclesCompleted = newCycles,
                remainingSeconds = breakDur,
                showCelebration = true,
                soundTrigger = if (it.soundEnabled) System.currentTimeMillis() else it.soundTrigger
            )
        }

        viewModelScope.launch {
            if (state.isFreeSession) {
                val saved = repository.saveFreeSession(userId, state.settings.focusMinutes)
                _uiState.update { it.copy(sessionToLink = saved) }
            } else {
                val habit = state.selectedHabit ?: return@launch
                when (habit.type) {
                    HabitType.TIMER -> {
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
    }

    private fun completeBreak() {
        pauseAtNextFocus()
    }

    /** Transition to FOCUS phase in paused state — user must tap to begin. */
    private fun pauseAtNextFocus() {
        val state = _uiState.value
        val focusDur = focusDurationSeconds(state.copy(phase = PomodoroPhase.FOCUS))
        _uiState.update {
            it.copy(
                isRunning = false,
                isInSession = true,
                targetEndTime = null,
                phase = PomodoroPhase.FOCUS,
                remainingSeconds = focusDur,
                showCelebration = false
            )
        }
    }

    fun checkResumeState() {
        val state = _uiState.value
        if (state.isRunning && state.targetEndTime != null) {
            val now = System.currentTimeMillis()
            if (now >= state.targetEndTime) {
                when (state.phase) {
                    PomodoroPhase.FOCUS -> completeFocusInterval()
                    PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> completeBreak()
                }
            }
        }
    }

}
