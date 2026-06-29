package com.example.habitpower.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.Checklist
import com.example.habitpower.data.model.ChecklistItem
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val selectedTab: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _userId = MutableStateFlow(-1L)

    init {
        viewModelScope.launch {
            prefsRepository.activeUserId.collect { uid ->
                _userId.value = uid ?: -1L
            }
        }
    }

    val taskLists: StateFlow<List<TaskList>> = _userId.flatMapLatest { uid ->
        if (uid > 0) repository.getTaskLists(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val checklists: StateFlow<List<Checklist>> = _userId.flatMapLatest { uid ->
        if (uid > 0) repository.getChecklists(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userId: StateFlow<Long> = _userId

    // ── Task Lists ──────────────────────────────────────────────────────────

    fun addTaskList(name: String) {
        val uid = _userId.value
        if (uid <= 0 || name.isBlank()) return
        viewModelScope.launch { repository.insertTaskList(TaskList(userId = uid, name = name.trim())) }
    }

    fun renameTaskList(list: TaskList, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.updateTaskList(list.copy(name = newName.trim())) }
    }

    fun deleteTaskList(list: TaskList) {
        viewModelScope.launch { repository.deleteTaskList(list) }
    }

    // ── Tasks ───────────────────────────────────────────────────────────────

    fun addTask(listId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertTask(Task(taskListId = listId, name = name.trim())) }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    // ── Checklists ──────────────────────────────────────────────────────────

    fun addChecklist(name: String, resetsDaily: Boolean = false) {
        val uid = _userId.value
        if (uid <= 0 || name.isBlank()) return
        viewModelScope.launch {
            repository.insertChecklist(Checklist(userId = uid, name = name.trim(), resetsDaily = resetsDaily))
        }
    }

    fun deleteChecklist(checklist: Checklist) {
        viewModelScope.launch { repository.deleteChecklist(checklist) }
    }

    fun resetChecklist(checklist: Checklist) {
        viewModelScope.launch { repository.resetChecklist(checklist.id) }
    }

    // ── Checklist Items ─────────────────────────────────────────────────────

    fun addChecklistItem(checklistId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertChecklistItem(ChecklistItem(checklistId = checklistId, name = name.trim()))
        }
    }

    fun toggleChecklistItem(item: ChecklistItem) {
        viewModelScope.launch { repository.toggleChecklistItem(item) }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        viewModelScope.launch { repository.deleteChecklistItem(item) }
    }

    fun getTasksForList(listId: Long) = repository.getTasksForList(listId)

    fun getItemsForChecklist(checklistId: Long) = repository.getItemsForChecklist(checklistId)
}
