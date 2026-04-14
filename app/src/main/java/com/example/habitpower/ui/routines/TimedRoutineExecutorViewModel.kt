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
import com.example.habitpower.data.model.WorkoutSession
import com.example.habitpower.util.SoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    var currentExerciseIndex by mutableIntStateOf(0)
        private set

    // Tracks the active countdown coroutine so it can be cleanly cancelled on skip/pause.
    private var executionJob: Job? = null
    private var startTime: Long = 0L

    init {
        viewModelScope.launch {
            val r = repository.getRoutineById(routineId)
            routine = r
            if (r != null && r.type == RoutineType.TIMED) {
                exercises = repository.getExercisesForRoutine(routineId)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.Eagerly,
                        initialValue = emptyList()
                    ).value
            }
        }
    }

    fun startRoutine() {
        if (exercises.isEmpty() || routine == null) return
        currentExerciseIndex = 0
        isRunning = true
        startTime = System.currentTimeMillis()
        executionJob = viewModelScope.launch {
            runExerciseCountdown(currentExerciseIndex, fromSeconds = exercises[0].targetDurationSeconds ?: 60)
        }
    }

    fun pauseRoutine() {
        // Cancel the active coroutine — currentPhase already holds the last-written timeRemaining,
        // so resume can pick up from exactly where we left off.
        executionJob?.cancel()
        executionJob = null
        isRunning = false
    }

    fun resumeRoutine() {
        if (exercises.isEmpty()) return
        isRunning = true
        executionJob = viewModelScope.launch {
            when (val phase = currentPhase) {
                is ExecutionPhase.ExercisePhase ->
                    runExerciseCountdown(phase.position, fromSeconds = phase.timeRemaining)
                is ExecutionPhase.RestPhase ->
                    runRestCountdown(fromSeconds = phase.timeRemaining)
                else -> { /* nothing to resume */ }
            }
        }
    }

    fun skipToNextExercise() {
        // Cancel current countdown so the old coroutine doesn't also advance the index.
        executionJob?.cancel()
        executionJob = null

        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
            isRunning = true
            val nextExercise = exercises[currentExerciseIndex]
            executionJob = viewModelScope.launch {
                runExerciseCountdown(currentExerciseIndex, fromSeconds = nextExercise.targetDurationSeconds ?: 60)
            }
        } else {
            finishRoutine()
        }
    }

    fun finishRoutine() {
        executionJob?.cancel()
        executionJob = null
        isRunning = false
        currentPhase = ExecutionPhase.Completed
        val endTime = System.currentTimeMillis()
        viewModelScope.launch {
            val r = routine
            if (r != null) {
                repository.insertSession(
                    WorkoutSession(
                        date = LocalDate.now(),
                        routineId = routineId,
                        routineName = r.name,
                        isCompleted = true,
                        startTime = startTime,
                        endTime = endTime
                    )
                )
            }
            repository.completeRoutineLinkedHabits(routineId)
        }
    }

    // Counts down one exercise, then chains into rest (if applicable) or the next exercise.
    private suspend fun runExerciseCountdown(index: Int, fromSeconds: Int) {
        val exercise = exercises.getOrNull(index) ?: return

        val playStartSound = repository.getRoutineStartSoundEnabled().first()
        val startSoundId = repository.getRoutineStartSoundId().first()
        val playEndSound = repository.getRoutineEndSoundEnabled().first()
        val endSoundId = repository.getRoutineEndSoundId().first()

        if (playStartSound) SoundPlayer.playById(startSoundId)

        for (timeRemaining in fromSeconds downTo 0) {
            currentPhase = ExecutionPhase.ExercisePhase(exercise, index, timeRemaining)
            delay(1000)
        }

        if (playEndSound) SoundPlayer.playById(endSoundId)

        val restTime = routine?.restTimeSeconds ?: 0
        if (index < exercises.size - 1) {
            if (restTime > 0) {
                runRestCountdown(fromSeconds = restTime)
            } else {
                // No rest — advance directly
                currentExerciseIndex = index + 1
                runExerciseCountdown(currentExerciseIndex, fromSeconds = exercises[currentExerciseIndex].targetDurationSeconds ?: 60)
            }
        } else {
            finishRoutine()
        }
    }

    // Counts down the rest period, then chains into the next exercise.
    private suspend fun runRestCountdown(fromSeconds: Int) {
        for (timeRemaining in fromSeconds downTo 0) {
            currentPhase = ExecutionPhase.RestPhase(timeRemaining)
            delay(1000)
        }
        val nextIndex = currentExerciseIndex + 1
        val nextExercise = exercises.getOrNull(nextIndex) ?: return
        currentExerciseIndex = nextIndex
        runExerciseCountdown(currentExerciseIndex, fromSeconds = nextExercise.targetDurationSeconds ?: 60)
    }
}
