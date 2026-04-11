package com.example.habitpower.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AdminAssignmentsViewModel(private val repository: HabitPowerRepository) : ViewModel() {
    private val selectedUserIdFlow = MutableStateFlow<Long?>(null)
    private val draftSelectedHabitIds = MutableStateFlow<Set<Long>?>(null)
    private val _saveSuccessTick = MutableStateFlow(0L)
    val saveSuccessTick: StateFlow<Long> = _saveSuccessTick

    val users: StateFlow<List<UserProfile>> = repository.getAllUsers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitDefinition>> = repository.getAllHabits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val selectedUserId: StateFlow<Long?> = selectedUserIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val assignedHabitIds = selectedUserIdFlow.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            repository.getAssignedHabitIdsForUser(userId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val selectedHabitIds: StateFlow<Set<Long>> = combine(assignedHabitIds, draftSelectedHabitIds) { assigned, draft ->
        draft ?: assigned.toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet()
    )

    init {
        viewModelScope.launch {
            if (selectedUserIdFlow.value == null) {
                selectedUserIdFlow.value = repository.getResolvedActiveUser().first()?.id
                    ?: users.value.firstOrNull()?.id
            }
        }
    }

    fun setSelectedUser(userId: Long) {
        selectedUserIdFlow.value = userId
        draftSelectedHabitIds.value = null
    }

    fun toggleHabit(habitId: Long, isSelected: Boolean) {
        val current = selectedHabitIds.value.toMutableSet()
        if (isSelected) {
            current.add(habitId)
        } else {
            current.remove(habitId)
        }
        draftSelectedHabitIds.value = current
    }

    fun saveAssignments() {
        val userId = selectedUserIdFlow.value ?: return
        val orderedIds = habits.value.map { it.id }.filter { selectedHabitIds.value.contains(it) }

        viewModelScope.launch {
            repository.replaceAssignmentsForUser(userId, orderedIds)
            draftSelectedHabitIds.value = null
            _saveSuccessTick.value += 1
        }
    }
}
