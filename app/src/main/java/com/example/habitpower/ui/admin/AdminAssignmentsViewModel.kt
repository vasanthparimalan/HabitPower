package com.example.habitpower.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.LifeArea
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
    private val draftSelectedLifeAreaIds = MutableStateFlow<Set<Long>?>(null)
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

    val lifeAreas: StateFlow<List<LifeArea>> = repository.getActiveLifeAreas().stateIn(
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

    private val assignedLifeAreaIds = selectedUserIdFlow.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            repository.getAssignedLifeAreaIdsForUser(userId)
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

    val selectedLifeAreaIds: StateFlow<Set<Long>> = combine(assignedLifeAreaIds, draftSelectedLifeAreaIds) { assigned, draft ->
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
        draftSelectedLifeAreaIds.value = null
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

    fun toggleLifeArea(lifeAreaId: Long, isSelected: Boolean) {
        val current = selectedLifeAreaIds.value.toMutableSet()
        if (isSelected) {
            current.add(lifeAreaId)
        } else {
            current.remove(lifeAreaId)
        }
        draftSelectedLifeAreaIds.value = current
    }

    fun saveAssignments() {
        val userId = selectedUserIdFlow.value ?: return
        val orderedIds = habits.value.map { it.id }.filter { selectedHabitIds.value.contains(it) }
        val orderedLifeAreaIds = lifeAreas.value.map { it.id }.filter { selectedLifeAreaIds.value.contains(it) }

        viewModelScope.launch {
            repository.replaceAssignmentsForUser(userId, orderedIds)
            repository.replaceLifeAreaAssignmentsForUser(userId, orderedLifeAreaIds)
            draftSelectedHabitIds.value = null
            draftSelectedLifeAreaIds.value = null
            _saveSuccessTick.value += 1
        }
    }
}
