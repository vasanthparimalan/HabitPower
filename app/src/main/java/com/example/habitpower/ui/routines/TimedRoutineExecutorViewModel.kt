package com.example.habitpower.ui.routines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.util.SoundPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ExecutionPhase {
    object Idle : ExecutionPhase()
    data class ExercisePhase(val exercise: Exercise, val position: Int, val timeRemaining: Int) : ExecutionPhase()
    data class RestPhase(val timeRemaining: Int) : ExecutionPhase()
    object Completed : ExecutionPhase()
}

class TimedRoutineExecutorViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: -1L

    var routine by mutableStateOf<Routine?>(null)
        private set
    var exercises by mutableStateOf<List<Exercise>>(emptyList())
        private set
    var currentPhase by mutableStateOf<ExecutionPhase>(ExecutionPhase.Idle)
        private set
    var isRunning by mutableStateOf(false)
        private set
    var currentExerciseIndex by mutableIntStateOf(-1)
        private set

    init {
        viewModelScope.launch {
            // Load routine and exercises
            val r = repository.getRoutineById(routineId)
            routine = r
            
            if (r != null && r.type == RoutineType.TIMED) {
                val exs = repository.getExercisesForRoutine(routineId)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.Eagerly,
                        initialValue = emptyList()
                    ).value
                exercises = exs
            }
        }
    }

    fun startRoutine() {
        if (exercises.isEmpty() || routine == null) return
        
        isRunning = true
        currentExerciseIndex = 0
        viewModelScope.launch {
            executeRoutineSequence()
        }
    }

    fun pauseRoutine() {
        isRunning = false
    }

    fun resumeRoutine() {
        if (exercises.isEmpty()) return
        isRunning = true
        viewModelScope.launch {
            executeRoutineSequence()
        }
    }

    fun skipToNextExercise() {
        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
        }
    }

    fun finishRoutine() {
        isRunning = false
        currentPhase = ExecutionPhase.Completed
        viewModelScope.launch {
            repository.completeRoutineLinkedHabits(routineId)
        }
    }

    private suspend fun executeRoutineSequence() {
        val exercise = exercises.getOrNull(currentExerciseIndex) ?: return
        val duration = exercise.targetDurationSeconds ?: 60
        val restTime = routine?.restTimeSeconds ?: 0

        val playStartSound = repository.getRoutineStartSoundEnabled().first()
        val startSoundId = repository.getRoutineStartSoundId().first()
        val playEndSound = repository.getRoutineEndSoundEnabled().first()
        val endSoundId = repository.getRoutineEndSoundId().first()

        if (playStartSound) {
            SoundPlayer.playById(startSoundId)
        }

        // Execute exercise phase
        for (timeRemaining in duration downTo 0) {
            if (!isRunning) break
            currentPhase = ExecutionPhase.ExercisePhase(exercise, currentExerciseIndex, timeRemaining)
            delay(1000) // 1 second tick
        }

        if (isRunning && playEndSound) {
            SoundPlayer.playById(endSoundId)
        }

        if (!isRunning) return

        // If there's a next exercise, show rest phase
        if (currentExerciseIndex < exercises.size - 1 && restTime > 0) {
            for (timeRemaining in restTime downTo 0) {
                if (!isRunning) break
                currentPhase = ExecutionPhase.RestPhase(timeRemaining)
                delay(1000) // 1 second tick
            }
        }

        if (!isRunning) return

        // Move to next exercise or complete
        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
            executeRoutineSequence()
        } else {
            finishRoutine()
        }
    }
}
