package com.example.habitpower.ui.exercises

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.util.ExercisePackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ImportPackViewModel(
    private val repository: HabitPowerRepository
) : ViewModel() {

    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        data class Preview(val items: List<ExercisePackManager.PackItem>) : ImportState()
        data class Done(val imported: Int, val skipped: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportState.Loading
            val items = ExercisePackManager.readFromUri(context, uri)
            _state.value = if (items.isEmpty()) ImportState.Error("No valid exercises found in this file.")
                           else ImportState.Preview(items)
        }
    }

    fun importAll(items: List<ExercisePackManager.PackItem>) {
        viewModelScope.launch {
            val existing = repository.getAllExercises().first()
                .map { it.name.trim().lowercase() }.toSet()

            var imported = 0
            var skipped = 0
            items.forEach { item ->
                if (existing.contains(item.name.trim().lowercase())) {
                    skipped++
                } else {
                    repository.insertExercise(
                        Exercise(
                            name = item.name,
                            description = item.description,
                            imageUri = null,
                            targetDurationSeconds = item.targetDurationSeconds,
                            targetReps = item.targetReps,
                            targetSets = item.targetSets,
                            instructions = item.instructions,
                            tags = item.tags,
                            category = item.category,
                            wgerExerciseId = item.wgerExerciseId
                        )
                    )
                    imported++
                }
            }
            _state.value = ImportState.Done(imported, skipped)
        }
    }

    fun reset() {
        _state.value = ImportState.Idle
    }
}
