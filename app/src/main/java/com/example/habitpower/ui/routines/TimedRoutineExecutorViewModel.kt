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
import com.example.habitpower.util.TtsPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
    var currentRound by mutableIntStateOf(1)
        private set
    var totalRounds by mutableIntStateOf(1)
        private set
    var ttsEnabled by mutableStateOf(false)
        private set

    // Tracks what index + round come after the current rest period, so that
    // pause → resume during rest lands at the correct next exercise.
    private var nextIndexAfterRest: Int = 0
    private var nextRoundAfterRest: Int = 1

    private var executionJob: Job? = null
    private var startTime: Long = 0L

    init {
        viewModelScope.launch {
            repository.getRoutineTtsEnabled().collect { ttsEnabled = it }
        }
        viewModelScope.launch {
            val r = repository.getRoutineById(routineId)
            routine = r
            if (r != null && r.type == RoutineType.TIMED) {
                totalRounds = r.repeatCount.coerceAtLeast(1)
                repository.getExercisesForRoutine(routineId).collect { loaded ->
                    exercises = loaded
                }
            }
        }
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun startRoutine() {
        if (exercises.isEmpty() || routine == null) return
        currentExerciseIndex = 0
        currentRound = 1
        isRunning = true
        startTime = System.currentTimeMillis()
        if (totalRounds > 1 && ttsEnabled) TtsPlayer.speak("Round 1")
        executionJob = viewModelScope.launch {
            runExerciseCountdown(0, exercises[0].targetDurationSeconds ?: 60)
        }
    }

    fun pauseRoutine() {
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
                    runExerciseCountdown(phase.position, phase.timeRemaining)
                is ExecutionPhase.RestPhase -> {
                    runRestCountdown(phase.timeRemaining)
                    advanceAfterRest()
                }
                else -> { /* nothing to resume */ }
            }
        }
    }

    fun skipToNextExercise() {
        executionJob?.cancel()
        executionJob = null

        val isLastInRound = currentExerciseIndex >= exercises.size - 1
        val isLastRound = currentRound >= totalRounds

        isRunning = true
        executionJob = viewModelScope.launch {
            when {
                !isLastInRound -> {
                    currentExerciseIndex++
                    runExerciseCountdown(currentExerciseIndex, exercises[currentExerciseIndex].targetDurationSeconds ?: 60)
                }
                !isLastRound -> {
                    currentRound++
                    currentExerciseIndex = 0
                    if (ttsEnabled) TtsPlayer.speak("Round $currentRound")
                    runExerciseCountdown(0, exercises[0].targetDurationSeconds ?: 60)
                }
                else -> finishRoutine()
            }
        }
    }

    fun toggleTts() {
        val newValue = !ttsEnabled
        ttsEnabled = newValue
        viewModelScope.launch { repository.saveRoutineTtsEnabled(newValue) }
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

    // ── Internal execution engine ─────────────────────────────────────────────

    private suspend fun runExerciseCountdown(index: Int, fromSeconds: Int) {
        val exercise = exercises.getOrNull(index) ?: return

        val playStartSound = repository.getRoutineStartSoundEnabled().first()
        val startSoundId = repository.getRoutineStartSoundId().first()
        val playEndSound = repository.getRoutineEndSoundEnabled().first()
        val endSoundId = repository.getRoutineEndSoundId().first()

        if (playStartSound) SoundPlayer.playById(startSoundId)
        if (ttsEnabled) {
            val durationText = exercise.targetDurationSeconds
                ?.let { s -> if (s >= 60) ", ${s / 60} minute${if (s / 60 > 1) "s" else ""}" else ", $s seconds" }
                ?: ""
            TtsPlayer.speak("${exercise.name}$durationText")
        }

        for (timeRemaining in fromSeconds downTo 0) {
            currentPhase = ExecutionPhase.ExercisePhase(exercise, index, timeRemaining)
            delay(1000)
        }

        if (playEndSound) SoundPlayer.playById(endSoundId)

        val restTime = routine?.restTimeSeconds ?: 0
        val isLastInRound = index >= exercises.size - 1
        val isLastRound = currentRound >= totalRounds

        when {
            !isLastInRound -> {
                // More exercises in this round
                nextIndexAfterRest = index + 1
                nextRoundAfterRest = currentRound
                if (restTime > 0) {
                    if (ttsEnabled) TtsPlayer.speak("Rest, $restTime seconds")
                    runRestCountdown(restTime)
                }
                currentExerciseIndex = nextIndexAfterRest
                runExerciseCountdown(currentExerciseIndex, exercises[currentExerciseIndex].targetDurationSeconds ?: 60)
            }
            !isLastRound -> {
                // End of round — more rounds to go
                nextIndexAfterRest = 0
                nextRoundAfterRest = currentRound + 1
                if (restTime > 0) {
                    if (ttsEnabled) TtsPlayer.speak("Rest, $restTime seconds")
                    runRestCountdown(restTime)
                }
                currentRound = nextRoundAfterRest
                currentExerciseIndex = 0
                if (ttsEnabled) TtsPlayer.speak("Round $currentRound")
                runExerciseCountdown(0, exercises[0].targetDurationSeconds ?: 60)
            }
            else -> finishRoutine()
        }
    }

    /** Pure countdown — no side-effects on what comes next. Caller decides. */
    private suspend fun runRestCountdown(fromSeconds: Int) {
        for (timeRemaining in fromSeconds downTo 0) {
            currentPhase = ExecutionPhase.RestPhase(timeRemaining)
            delay(1000)
        }
    }

    /** Called after rest completes (both in normal flow and after resume-from-rest). */
    private suspend fun advanceAfterRest() {
        currentRound = nextRoundAfterRest
        currentExerciseIndex = nextIndexAfterRest
        val exercise = exercises.getOrNull(currentExerciseIndex) ?: return
        if (ttsEnabled && nextIndexAfterRest == 0 && nextRoundAfterRest > 1) {
            TtsPlayer.speak("Round $currentRound")
        }
        runExerciseCountdown(currentExerciseIndex, exercise.targetDurationSeconds ?: 60)
    }
}
