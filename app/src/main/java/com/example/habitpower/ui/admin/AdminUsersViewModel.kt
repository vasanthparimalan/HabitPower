package com.example.habitpower.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminUsersViewModel(private val repository: HabitPowerRepository) : ViewModel() {
    private val _createSuccessTick = MutableStateFlow(0L)
    val createSuccessTick: StateFlow<Long> = _createSuccessTick

    var newUserName by mutableStateOf("")
        private set

    val users: StateFlow<List<UserProfile>> = repository.getAllUsers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeUser: StateFlow<UserProfile?> = repository.getResolvedActiveUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun updateNewUserName(value: String) {
        newUserName = value
    }

    fun createUser() {
        val name = newUserName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val userId = repository.createUser(name)
            if (activeUser.value == null) {
                repository.saveActiveUserId(userId)
            }
            newUserName = ""
            _createSuccessTick.value += 1
        }
    }

    fun setActiveUser(userId: Long) {
        viewModelScope.launch {
            repository.saveActiveUserId(userId)
        }
    }

    fun updateUser(user: UserProfile, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == user.name) return
        viewModelScope.launch {
            repository.updateUser(user.copy(name = trimmed))
        }
    }

    fun deleteUser(user: UserProfile) {
        viewModelScope.launch {
            repository.deleteUser(user)
            if (activeUser.value?.id == user.id) {
                val remainingUsers = users.value.filter { it.id != user.id }
                if (remainingUsers.isNotEmpty()) {
                    repository.saveActiveUserId(remainingUsers.first().id)
                }
            }
        }
    }
}
