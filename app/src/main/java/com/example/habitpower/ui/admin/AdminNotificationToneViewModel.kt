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
    val availableOptions: List<NotificationSoundOption> = NotificationSoundOption.entries
)

class AdminNotificationToneViewModel(
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationToneState())
    val state: StateFlow<NotificationToneState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefsRepository.soundEnabled,
                prefsRepository.notificationSoundId
            ) { enabled, soundId -> enabled to soundId }
                .collect { (enabled, soundId) ->
                    _state.update { it.copy(soundEnabled = enabled, selectedSoundId = soundId) }
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
}
