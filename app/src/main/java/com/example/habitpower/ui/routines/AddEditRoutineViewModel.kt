package com.example.habitpower.ui.routines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddEditRoutineViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.get<String>("routineId")?.toLongOrNull()?.takeIf { it != -1L }

    var name by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var routineType by mutableStateOf(RoutineType.NORMAL)
        private set
    var restTimeSeconds by mutableStateOf("")
        private set  // For timed routines

    var isSaving by mutableStateOf(false)
        private set

    // Current Exercises in the Routine
    private val _addedExercises = mutableStateListOf<Exercise>()
    val addedExercises: List<Exercise> = _addedExercises

    // All Available Exercises for Selection
    val allExercises: StateFlow<List<Exercise>> = repository.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        if (routineId != null) {
            viewModelScope.launch {
                // Load routine metadata
                val routine = repository.getRoutineById(routineId)
                routine?.let {
                    name = it.name
                    description = it.description
                    routineType = it.type
                    restTimeSeconds = it.restTimeSeconds.toString()
                }
                // Load initially-assigned exercises ONE TIME (first emission).
                // We do NOT use collect() here to avoid the live-update overwriting
                // any in-progress user edits when another coroutine emits a new list.
                val exercises = repository.getExercisesForRoutine(routineId).stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                ).value
                _addedExercises.clear()
                _addedExercises.addAll(exercises)
            }
        }
    }

    fun updateName(input: String) { name = input }
    fun updateDescription(input: String) { description = input }
    fun updateRoutineType(type: RoutineType) { routineType = type }
    fun updateRestTimeSeconds(input: String) { restTimeSeconds = input }

    fun addExercise(exercise: Exercise) {
        if (_addedExercises.none { it.id == exercise.id }) {
            _addedExercises.add(exercise)
        }
    }

    fun removeExercise(exercise: Exercise) {
        _addedExercises.remove(exercise)
    }

    fun moveExercise(from: Int, to: Int) {
        if (from in _addedExercises.indices && to in _addedExercises.indices) {
            val item = _addedExercises.removeAt(from)
            _addedExercises.add(to, item)
        }
    }

    /**
     * Saves the routine and all its exercise cross-refs atomically, then calls [onSaved].
     * Navigation should only happen inside [onSaved] so we don't race the DB write.
     */
    fun saveRoutine(onSaved: () -> Unit) {
        if (name.isBlank()) return
        if (isSaving) return

        isSaving = true
        viewModelScope.launch {
            try {
                val routine = Routine(
                    id = routineId ?: 0,
                    name = name.trim(),
                    description = description.trim(),
                    type = routineType,
                    restTimeSeconds = restTimeSeconds.toIntOrNull() ?: 0
                )

                val savedId = if (routineId == null) {
                    repository.insertRoutine(routine)
                } else {
                    repository.updateRoutine(routine)
                    routineId
                }

                // Atomically replace all cross-refs
                repository.clearRoutineExercises(savedId)
                _addedExercises.forEachIndexed { index, exercise ->
                    repository.addExerciseToRoutine(savedId, exercise.id, index)
                }

                onSaved()
            } finally {
                isSaving = false
            }
        }
    }
}
