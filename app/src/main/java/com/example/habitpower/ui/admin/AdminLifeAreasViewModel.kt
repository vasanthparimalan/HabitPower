package com.example.habitpower.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.repository.LifeAreaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminLifeAreasViewModel(private val repository: LifeAreaRepository) : ViewModel() {
    private val _createSuccessTick = MutableStateFlow(0L)
    val createSuccessTick: StateFlow<Long> = _createSuccessTick

    var newName by mutableStateOf("")
        private set

    var newDescription by mutableStateOf("")
        private set

    val lifeAreas: StateFlow<List<LifeArea>> = repository.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun updateNewName(value: String) {
        newName = value
    }

    fun updateNewDescription(value: String) {
        newDescription = value
    }

    fun createLifeArea() {
        val name = newName.trim()
        if (name.isBlank()) return
        val desc = newDescription.trim().ifBlank { null }
        viewModelScope.launch {
            val nextOrder = (lifeAreas.value.maxOfOrNull { it.displayOrder } ?: -1) + 1
            repository.create(LifeArea(name = name, description = desc, displayOrder = nextOrder))
            newName = ""
            newDescription = ""
            _createSuccessTick.value += 1
        }
    }

    fun updateLifeArea(l: LifeArea, name: String, description: String?) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || (trimmed == l.name && (description ?: "") == (l.description ?: ""))) return
        viewModelScope.launch {
            repository.update(l.copy(name = trimmed, description = description))
        }
    }

    fun deleteLifeArea(l: LifeArea) {
        viewModelScope.launch {
            repository.delete(l)
        }
    }
}
