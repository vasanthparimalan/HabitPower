package com.example.habitpower.ui.welcome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MissedDayWelcomeViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val daysAbsent: Long = savedStateHandle.get<String>("daysAbsent")?.toLongOrNull() ?: 2L

    val activeHabits = prefsRepository.activeUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            repository.getAssignedHabitsForUser(userId).map { habits ->
                habits.filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markOpenedToday() {
        viewModelScope.launch {
            prefsRepository.saveLastOpenedEpochDay(LocalDate.now().toEpochDay())
        }
    }
}
