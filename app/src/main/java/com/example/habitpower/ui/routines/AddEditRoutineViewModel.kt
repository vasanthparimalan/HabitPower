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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineEntryDraft(
    val localId: Long,
    val exercise: Exercise,
    val sets: Int? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null
)

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
        private set
    var repeatCount by mutableStateOf("1")
        private set

    var isSaving by mutableStateOf(false)
        private set

    private val _addedExercises = mutableStateListOf<RoutineEntryDraft>()
    val addedExercises: List<RoutineEntryDraft> = _addedExercises

    val allExercises: StateFlow<List<Exercise>> = repository.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        if (routineId != null) {
            viewModelScope.launch {
                val routine = repository.getRoutineById(routineId)
                routine?.let {
                    name = it.name
                    description = it.description
                    routineType = it.type
                    restTimeSeconds = it.restTimeSeconds.toString()
                    repeatCount = it.repeatCount.toString()
                }
                val entries = repository.getRoutineExercisesWithDetails(routineId).first()
                _addedExercises.clear()
                _addedExercises.addAll(entries.map { entry ->
                    RoutineEntryDraft(
                        localId = entry.crossRef.id,
                        exercise = entry.exercise,
                        sets = entry.crossRef.sets,
                        reps = entry.crossRef.reps,
                        durationSeconds = entry.crossRef.durationSeconds
                    )
                })
            }
        }
    }

    fun updateName(input: String) { name = input }
    fun updateDescription(input: String) { description = input }
    fun updateRoutineType(type: RoutineType) { routineType = type }
    fun updateRestTimeSeconds(input: String) { restTimeSeconds = input }
    fun updateRepeatCount(input: String) { repeatCount = input }

    fun addExercise(exercise: Exercise) {
        _addedExercises.add(
            RoutineEntryDraft(
                localId = System.currentTimeMillis() + _addedExercises.size,
                exercise = exercise,
                sets = null,
                reps = null,
                durationSeconds = null
            )
        )
    }

    fun removeExerciseEntry(localId: Long) {
        _addedExercises.removeAll { it.localId == localId }
    }

    fun moveExercise(from: Int, to: Int) {
        if (from in _addedExercises.indices && to in _addedExercises.indices) {
            val item = _addedExercises.removeAt(from)
            _addedExercises.add(to, item)
        }
    }

    fun updateEntrySpecs(localId: Long, sets: Int?, reps: Int?, durationSeconds: Int?) {
        val index = _addedExercises.indexOfFirst { it.localId == localId }
        if (index >= 0) {
            _addedExercises[index] = _addedExercises[index].copy(sets = sets, reps = reps, durationSeconds = durationSeconds)
        }
    }

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
                    restTimeSeconds = restTimeSeconds.toIntOrNull() ?: 0,
                    repeatCount = (repeatCount.toIntOrNull() ?: 1).coerceAtLeast(1)
                )

                val savedId = if (routineId == null) {
                    repository.insertRoutine(routine)
                } else {
                    repository.updateRoutine(routine)
                    routineId
                }

                repository.clearRoutineExercises(savedId)
                _addedExercises.forEachIndexed { index, entry ->
                    repository.addExerciseToRoutine(
                        routineId = savedId,
                        exerciseId = entry.exercise.id,
                        order = index,
                        sets = entry.sets,
                        reps = entry.reps,
                        durationSeconds = entry.durationSeconds
                    )
                }

                onSaved()
            } finally {
                isSaving = false
            }
        }
    }
}
