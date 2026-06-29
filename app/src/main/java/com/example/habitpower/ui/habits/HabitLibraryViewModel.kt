package com.example.habitpower.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitTemplate
import com.example.habitpower.data.model.HabitTemplates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitLibraryUiState(
    val selectedArchetype: HabitTemplate.Archetype? = null,
    val addedNames: Set<String> = emptySet(),
    val isAdding: Boolean = false,
    val snackbarMessage: String? = null
)

class HabitLibraryViewModel(
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitLibraryUiState())
    val uiState: StateFlow<HabitLibraryUiState> = _uiState.asStateFlow()

    val templates: List<HabitTemplate> = HabitTemplates.all

    init {
        viewModelScope.launch {
            repository.getAllHabits().collect { habits ->
                _uiState.update { state ->
                    state.copy(addedNames = habits.map { it.name.lowercase() }.toSet())
                }
            }
        }
    }

    fun setArchetype(archetype: HabitTemplate.Archetype?) {
        _uiState.update { it.copy(selectedArchetype = archetype) }
    }

    fun addTemplate(template: HabitTemplate) {
        if (_uiState.value.isAdding) return
        _uiState.update { it.copy(isAdding = true) }
        viewModelScope.launch {
            val added = repository.addHabitFromTemplate(template)
            _uiState.update {
                it.copy(
                    isAdding = false,
                    snackbarMessage = if (added) "${template.name} added." else "${template.name} is already in your habits."
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
