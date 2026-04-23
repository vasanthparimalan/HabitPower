package com.example.habitpower.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.ExerciseLibraryRepository
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.ExerciseLibraryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryBrowseViewModel(
    private val libraryRepository: ExerciseLibraryRepository,
    private val habitPowerRepository: HabitPowerRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<ExerciseCategory?>(null)

    private val allItems: List<ExerciseLibraryItem> = libraryRepository.getAll()

    // Names already in Room (by lowercase) — used to show "Added" state
    val addedNames: StateFlow<Set<String>> = habitPowerRepository.getAllExercises()
        .map { list -> list.map { it.name.trim().lowercase() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val filteredItems: StateFlow<List<ExerciseLibraryItem>> =
        combine(searchQuery, selectedCategory) { query, cat ->
            allItems.filter { item ->
                val matchesQuery = query.isBlank() ||
                        item.name.contains(query, ignoreCase = true) ||
                        item.primaryMuscle?.contains(query, ignoreCase = true) == true
                val matchesCat = cat == null || item.category == cat
                matchesQuery && matchesCat
            }.sortedBy { it.name.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), allItems.sortedBy { it.name.lowercase() })

    fun addToMyExercises(item: ExerciseLibraryItem) {
        viewModelScope.launch {
            habitPowerRepository.insertExercise(
                Exercise(
                    name = item.name,
                    description = item.primaryMuscle ?: "",
                    imageUri = item.imageUri,
                    targetDurationSeconds = null,
                    targetReps = null,
                    targetSets = null,
                    notes = null,
                    instructions = item.instructions,
                    tags = item.tags,
                    category = item.category,
                    wgerExerciseId = item.wgerExerciseId
                )
            )
        }
    }

    fun removeFromMyExercises(exerciseName: String) {
        viewModelScope.launch {
            val exercises = habitPowerRepository.getAllExercises()
            exercises.collect { list ->
                list.firstOrNull { it.name.trim().equals(exerciseName.trim(), ignoreCase = true) }
                    ?.let { habitPowerRepository.deleteExercise(it) }
                return@collect
            }
        }
    }
}

private val ExerciseLibraryItem.tags: String
    get() = category.name.lowercase().replace("_other", "")
