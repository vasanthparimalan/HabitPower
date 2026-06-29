package com.example.habitpower.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DriveSyncUiState(
    val accountName: String? = null,
    val lastSyncAt: Long? = null,
    val isSyncing: Boolean = false,
    val message: String? = null
)

class DriveSyncViewModel(
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DriveSyncUiState> = combine(
        prefsRepository.driveAccountName,
        prefsRepository.driveLastSyncAt,
        _isSyncing,
        _message
    ) { accountName, lastSyncAt, isSyncing, message ->
        DriveSyncUiState(accountName, lastSyncAt, isSyncing, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriveSyncUiState())

    fun saveAccount(name: String) {
        viewModelScope.launch {
            prefsRepository.setDriveAccount(name)
            _message.value = "Connected as $name"
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            prefsRepository.setDriveAccount(null)
            _message.value = null
            _isSyncing.value = false
        }
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    fun setMessage(msg: String?) {
        _message.value = msg
    }
}
