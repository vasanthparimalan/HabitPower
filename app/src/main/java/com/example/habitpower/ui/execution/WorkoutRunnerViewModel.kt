package com.example.habitpower.ui.execution

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.WorkoutSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkoutRunnerViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: -1L

    private var exercises = listOf<Exercise>()

    var currentExerciseIndex by mutableStateOf(0)
        private set

    var currentExercise by mutableStateOf<Exercise?>(null)
        private set

    var isWorkoutComplete by mutableStateOf(false)
        private set

    // Timer logic
    var timerSeconds by mutableStateOf(0)
        private set
    var isTimerRunning by mutableStateOf(false)
        private set

    private var startTime: Long = 0

    init {
        if (routineId != -1L) {
            viewModelScope.launch {
                repository.getExercisesForRoutine(routineId).collect {
                    exercises = it
                    if (exercises.isNotEmpty() && currentExercise == null) {
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
        // Start timer if first exercise has duration? Or just let user start
        startTimer()
    }

    private fun startTimer() {
        isTimerRunning = true
        viewModelScope.launch {
            while (isTimerRunning) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    fun toggleTimer() {
        isTimerRunning = !isTimerRunning
        if (isTimerRunning) {
            startTimer()
        }
    }

    fun nextExercise() {
        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
            currentExercise = exercises[currentExerciseIndex]
            // Reset timer for next exercise if we want granular tracking,
            // but for now let's keep one global timer or per-exercise?
            // Requirement says "show done or pause". Let's reset timer for visualization.
            timerSeconds = 0
        } else {
            finishWorkout()
        }
    }

    private fun finishWorkout() {
        isWorkoutComplete = true
        isTimerRunning = false
        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId)
            repository.insertSession(
                WorkoutSession(
                    date = LocalDate.now(),
                    routineId = routineId,
                    routineName = routine?.name ?: "Unknown Routine",
                    isCompleted = true,
                    startTime = startTime,
                    endTime = System.currentTimeMillis()
                )
            )
        }
    }
}
