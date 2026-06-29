package com.example.habitpower.ui.chant

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.ChantSession
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChantUiState(
    val chants: List<ChantDefinition> = emptyList(),
    val selectedChant: ChantDefinition? = null,
    val isInSession: Boolean = false,
    val currentCount: Int = 0,
    val targetCount: Int = 108,
    val sessionStartMs: Long = 0L,
    val isComplete: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingChant: ChantDefinition? = null,
    val userId: Long = -1L,
    val autoMode: Boolean = false,
    val autoIntervalSeconds: Int = 3,
    val autoSoundId: String = "positive",
    val linkedHabitName: String? = null,
    val audioUnavailable: Boolean = false
)

class ChantViewModel(
    private val application: Application,
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChantUiState())
    val uiState: StateFlow<ChantUiState> = _uiState.asStateFlow()

    private var autoJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    val habitsForLinking: StateFlow<List<HabitDefinition>> = repository.getAllHabits()
        .map { habits -> habits.filter { it.type != HabitType.TEXT && it.type != HabitType.ROUTINE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            prefsRepository.activeUserId.collect { uid ->
                _uiState.update { it.copy(userId = uid ?: -1L) }
            }
        }
        viewModelScope.launch {
            repository.getAllChants().collect { chants ->
                _uiState.update { it.copy(chants = chants) }
            }
        }
    }

    fun selectChant(chant: ChantDefinition) {
        _uiState.update { it.copy(selectedChant = chant, targetCount = chant.defaultCount) }
    }

    fun beginSession() {
        val state = _uiState.value
        if (state.selectedChant == null) return
        _uiState.update {
            it.copy(
                isInSession = true,
                currentCount = 0,
                sessionStartMs = System.currentTimeMillis(),
                isComplete = false,
                linkedHabitName = null
            )
        }
        val audioUri = state.selectedChant.audioUri
        if (audioUri != null) {
            _uiState.update { it.copy(audioUnavailable = false) }
            startAudioRepeater(audioUri)
        } else if (_uiState.value.autoMode) {
            startAutoTimer()
        }
    }

    fun incrementCount() {
        val state = _uiState.value
        if (!state.isInSession || state.isComplete) return
        if (!state.autoMode && state.selectedChant?.audioUri == null) {
            com.example.habitpower.util.SoundPlayer.playById(state.autoSoundId, 75)
        }
        val next = state.currentCount + 1
        if (next >= state.targetCount) {
            stopAutoTimer()
            stopAudioRepeater(_restart = false)
            _uiState.update { it.copy(currentCount = state.targetCount, isComplete = true, isInSession = false) }
            logSession(state.targetCount, state)
        } else {
            _uiState.update { it.copy(currentCount = next) }
        }
    }

    fun endSessionEarly() {
        stopAutoTimer()
        stopAudioRepeater(_restart = false)
        val state = _uiState.value
        if (!state.isInSession) return
        val count = state.currentCount
        if (count > 0) logSession(count, state)
        _uiState.update { it.copy(isInSession = false, isComplete = count >= state.targetCount) }
    }

    fun resetToPickerAfterDone() {
        stopAutoTimer()
        stopAudioRepeater(_restart = false)
        _uiState.update {
            it.copy(isComplete = false, selectedChant = null, currentCount = 0, autoMode = false, linkedHabitName = null)
        }
    }

    fun setAutoMode(enabled: Boolean) {
        _uiState.update { it.copy(autoMode = enabled) }
        val audioUri = _uiState.value.selectedChant?.audioUri
        if (_uiState.value.isInSession && audioUri == null) {
            if (enabled) startAutoTimer() else stopAutoTimer()
        }
    }

    fun setAutoInterval(seconds: Int) {
        _uiState.update { it.copy(autoIntervalSeconds = seconds.coerceIn(1, 30)) }
        if (_uiState.value.isInSession && _uiState.value.autoMode) {
            stopAutoTimer()
            startAutoTimer()
        }
    }

    fun setAutoSound(soundId: String) {
        _uiState.update { it.copy(autoSoundId = soundId) }
    }

    // ── Audio repeater ────────────────────────────────────────────────────────

    private fun startAudioRepeater(audioUri: String) {
        stopAudioRepeater(_restart = false)
        try {
            val mp = MediaPlayer().apply {
                setDataSource(application, Uri.parse(audioUri))
                prepare()
                setOnCompletionListener {
                    val state = _uiState.value
                    if (state.isInSession && !state.isComplete) {
                        incrementCount()
                        if (!_uiState.value.isComplete) start()
                    }
                }
            }
            mp.start()
            mediaPlayer = mp
        } catch (_: Exception) {
            _uiState.update { it.copy(audioUnavailable = true) }
        }
    }

    private fun stopAudioRepeater(_restart: Boolean) {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    // ── Auto timer ────────────────────────────────────────────────────────────

    private fun startAutoTimer() {
        stopAutoTimer()
        val intervalMs = (_uiState.value.autoIntervalSeconds * 1000L)
        autoJob = viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                val state = _uiState.value
                if (!state.isInSession || state.isComplete) break
                com.example.habitpower.util.SoundPlayer.playById(state.autoSoundId, 75)
                incrementCount()
            }
        }
    }

    private fun stopAutoTimer() {
        autoJob?.cancel()
        autoJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoTimer()
        stopAudioRepeater(_restart = false)
    }

    // ── Chant CRUD ────────────────────────────────────────────────────────────

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun dismissAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }

    fun saveCustomChant(name: String, text: String, tradition: String, count: Int, audioUri: String?) {
        viewModelScope.launch {
            repository.insertChant(
                ChantDefinition(
                    name = name.trim(),
                    text = text.trim(),
                    tradition = tradition.trim().ifBlank { null },
                    defaultCount = count.coerceIn(1, 9999),
                    isBuiltIn = false,
                    audioUri = audioUri
                )
            )
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun showEditFor(chant: ChantDefinition) {
        _uiState.update { it.copy(editingChant = chant) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(editingChant = null) }
    }

    fun updateCustomChant(name: String, text: String, tradition: String, count: Int, audioUri: String?) {
        val chant = _uiState.value.editingChant ?: return
        if (chant.isBuiltIn) return
        viewModelScope.launch {
            repository.updateChant(
                chant.copy(
                    name = name.trim(),
                    text = text.trim(),
                    tradition = tradition.trim().ifBlank { null },
                    defaultCount = count.coerceIn(1, 9999),
                    audioUri = audioUri
                )
            )
            _uiState.update { it.copy(editingChant = null) }
        }
    }

    fun deleteCustomChant(chant: ChantDefinition) {
        viewModelScope.launch { repository.deleteChant(chant) }
    }

    // ── Session-to-habit linking ──────────────────────────────────────────────

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
            _uiState.update { it.copy(linkedHabitName = habit.name) }
        }
    }

    private fun logSession(count: Int, state: ChantUiState) {
        val chantId = state.selectedChant?.id ?: return
        val userId = state.userId
        if (userId <= 0L) return
        val durationSec = (System.currentTimeMillis() - state.sessionStartMs) / 1000L
        viewModelScope.launch {
            repository.insertChantSession(
                ChantSession(
                    userId = userId,
                    chantId = chantId,
                    targetCount = state.targetCount,
                    actualCount = count,
                    durationSeconds = durationSec
                )
            )
        }
    }
}
