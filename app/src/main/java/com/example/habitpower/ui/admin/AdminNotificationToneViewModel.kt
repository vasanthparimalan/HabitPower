package com.example.habitpower.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.util.NotificationSoundOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationToneState(
    val soundEnabled: Boolean = true,
    val selectedSoundId: String = NotificationSoundOption.SHORT_BEEP.id,
    val routineStartSoundEnabled: Boolean = true,
    val routineStartSoundId: String = NotificationSoundOption.SHORT_BEEP.id,
    val routineEndSoundEnabled: Boolean = true,
    val routineEndSoundId: String = NotificationSoundOption.POSITIVE.id,
    val availableOptions: List<NotificationSoundOption> = NotificationSoundOption.entries
)

data class RoutineSoundSettings(
    val startEnabled: Boolean,
    val startSoundId: String,
    val endEnabled: Boolean,
    val endSoundId: String
)

class AdminNotificationToneViewModel(
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationToneState())
    val state: StateFlow<NotificationToneState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    prefsRepository.soundEnabled,
                    prefsRepository.notificationSoundId
                ) { enabled, soundId -> enabled to soundId },
                combine(
                    prefsRepository.routineStartSoundEnabled,
                    prefsRepository.routineStartSoundId,
                    prefsRepository.routineEndSoundEnabled,
                    prefsRepository.routineEndSoundId
                ) { routineStartEnabled, routineStartId, routineEndEnabled, routineEndId ->
                    RoutineSoundSettings(
                        startEnabled = routineStartEnabled,
                        startSoundId = routineStartId,
                        endEnabled = routineEndEnabled,
                        endSoundId = routineEndId
                    )
                }
            ) { completionSound, routineSounds ->
                NotificationToneState(
                    soundEnabled = completionSound.first,
                    selectedSoundId = completionSound.second,
                    routineStartSoundEnabled = routineSounds.startEnabled,
                    routineStartSoundId = routineSounds.startSoundId,
                    routineEndSoundEnabled = routineSounds.endEnabled,
                    routineEndSoundId = routineSounds.endSoundId,
                    availableOptions = NotificationSoundOption.entries
                )
            }.collect { state ->
                _state.value = state
            }
        }
    }

    fun toggleSound() {
        val new = !_state.value.soundEnabled
        _state.update { it.copy(soundEnabled = new) }
        viewModelScope.launch { prefsRepository.saveSoundEnabled(new) }
    }

    fun selectSound(option: NotificationSoundOption) {
        _state.update { it.copy(selectedSoundId = option.id) }
        viewModelScope.launch { prefsRepository.saveNotificationSoundId(option.id) }
    }

    fun toggleRoutineStartSound() {
        val newValue = !_state.value.routineStartSoundEnabled
        _state.update { it.copy(routineStartSoundEnabled = newValue) }
        viewModelScope.launch { prefsRepository.saveRoutineStartSoundEnabled(newValue) }
    }

    fun selectRoutineStartSound(option: NotificationSoundOption) {
        _state.update { it.copy(routineStartSoundId = option.id) }
        viewModelScope.launch { prefsRepository.saveRoutineStartSoundId(option.id) }
    }

    fun toggleRoutineEndSound() {
        val newValue = !_state.value.routineEndSoundEnabled
        _state.update { it.copy(routineEndSoundEnabled = newValue) }
        viewModelScope.launch { prefsRepository.saveRoutineEndSoundEnabled(newValue) }
    }

    fun selectRoutineEndSound(option: NotificationSoundOption) {
        _state.update { it.copy(routineEndSoundId = option.id) }
        viewModelScope.launch { prefsRepository.saveRoutineEndSoundId(option.id) }
    }
}
