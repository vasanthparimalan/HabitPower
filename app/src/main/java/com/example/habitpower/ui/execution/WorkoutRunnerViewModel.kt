package com.example.habitpower.ui.execution

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkoutRunnerViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: -1L

    private var exercises = listOf<Exercise>()

    var currentExerciseIndex by mutableIntStateOf(0)
        private set

    var totalExercises by mutableIntStateOf(0)
        private set

    var currentExercise by mutableStateOf<Exercise?>(null)
        private set

    val isLastExercise: Boolean
        get() = currentExerciseIndex >= exercises.size - 1

    var isWorkoutComplete by mutableStateOf(false)
        private set

    var timerSeconds by mutableIntStateOf(0)
        private set
    var isTimerRunning by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var startTime: Long = 0

    init {
        if (routineId != -1L) {
            viewModelScope.launch {
                repository.getExercisesForRoutine(routineId).collect { loaded ->
                    exercises = loaded
                    totalExercises = loaded.size
                    if (loaded.isNotEmpty() && currentExercise == null) {
                        startWorkout()
                    }
                }
            }
        }
    }

    private fun startWorkout() {
        if (exercises.isEmpty()) return
        currentExercise = exercises[0]
        currentExerciseIndex = 0
        startTime = System.currentTimeMillis()
    }

    fun toggleTimer() {
        if (isTimerRunning) {
            timerJob?.cancel()
            timerJob = null
            isTimerRunning = false
        } else {
            isTimerRunning = true
            timerJob = viewModelScope.launch {
                while (isTimerRunning) {
                    delay(1000)
                    timerSeconds++
                }
            }
        }
    }

    fun nextExercise() {
        // Reset per-exercise timer
        timerJob?.cancel()
        timerJob = null
        isTimerRunning = false
        timerSeconds = 0

        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
            currentExercise = exercises[currentExerciseIndex]
        } else {
            finishWorkout()
        }
    }

    private fun finishWorkout() {
        isWorkoutComplete = true
        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId)
            repository.insertSession(
                WorkoutSession(
                    date = LocalDate.now(),
                    routineId = routineId,
                    routineName = routine?.name ?: "Routine",
                    isCompleted = true,
                    startTime = startTime,
                    endTime = System.currentTimeMillis()
                )
            )
            repository.completeRoutineLinkedHabits(routineId)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
