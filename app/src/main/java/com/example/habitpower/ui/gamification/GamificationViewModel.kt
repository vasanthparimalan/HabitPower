package com.example.habitpower.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GamificationUiState(
    val streak: Int = 0,
    val longestStreak: Int = 0
)

class GamificationViewModel(
    private val habitPowerRepository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    init {
        observeActiveUser()
    }

    private fun observeActiveUser() {
        viewModelScope.launch {
            habitPowerRepository.getResolvedActiveUser().collectLatest { user ->
                if (user == null) return@collectLatest
                gamificationRepository.observeStats(user.id).collectLatest { stats ->
                    _uiState.value = GamificationUiState(
                        streak = stats.currentStreak,
                        longestStreak = stats.longestStreak
                    )
                }
            }
        }
    }

    fun onCheckInSaved(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val user = habitPowerRepository.getResolvedActiveUser().first() ?: return@launch
            gamificationRepository.onDayCheckedIn(user.id, date)
        }
    }
}
