package com.example.habitpower.ui.execution

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.RoutineExerciseWithDetails
import com.example.habitpower.data.model.WorkoutSession
import com.example.habitpower.util.SoundPlayer
import com.example.habitpower.util.TtsPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkoutRunnerViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: -1L

    var routineName by mutableStateOf("")
        private set
    var allExercises by mutableStateOf<List<RoutineExerciseWithDetails>>(emptyList())
        private set

    var currentExerciseIndex by mutableIntStateOf(0)
        private set
    var totalExercises by mutableIntStateOf(0)
        private set
    var currentExercise by mutableStateOf<RoutineExerciseWithDetails?>(null)
        private set

    val isLastExercise: Boolean
        get() = currentExerciseIndex >= allExercises.size - 1

    var isStarted by mutableStateOf(false)
        private set

    var isWorkoutComplete by mutableStateOf(false)
        private set

    var linkedHabitName by mutableStateOf<String?>(null)
        private set

    val habitsForLinking: StateFlow<List<HabitDefinition>> = repository.getAllHabits()
        .map { habits -> habits.filter { it.type != HabitType.TEXT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var timerSeconds by mutableIntStateOf(0)
        private set
    var isTimerRunning by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var startTime: Long = 0

    init {
        if (routineId != -1L) {
            viewModelScope.launch {
                val routine = repository.getRoutineById(routineId)
                routineName = routine?.name ?: ""
            }
            viewModelScope.launch {
                repository.getRoutineExercisesWithDetails(routineId).collect { loaded ->
                    allExercises = loaded
                    totalExercises = loaded.size
                    if (loaded.isNotEmpty() && currentExercise == null) {
                        currentExercise = loaded[0]
                    }
                }
            }
        }
    }

    fun confirmStart() {
        if (allExercises.isEmpty()) return
        isStarted = true
        currentExerciseIndex = 0
        currentExercise = allExercises[0]
        startTime = System.currentTimeMillis()
        announceExercise(allExercises[0])
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
        timerJob?.cancel()
        timerJob = null
        isTimerRunning = false
        timerSeconds = 0

        if (currentExerciseIndex < allExercises.size - 1) {
            currentExerciseIndex++
            currentExercise = allExercises[currentExerciseIndex]
            announceExercise(allExercises[currentExerciseIndex])
        } else {
            announceComplete()
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

    private fun announceExercise(entry: RoutineExerciseWithDetails) {
        viewModelScope.launch {
            val soundEnabled = repository.getRoutineStartSoundEnabled().first()
            val soundId = repository.getRoutineStartSoundId().first()
            val ttsOn = repository.getRoutineTtsEnabled().first()
            if (soundEnabled) SoundPlayer.playById(soundId)
            if (ttsOn) TtsPlayer.speak(entry.exercise.name)
        }
    }

    private fun announceComplete() {
        viewModelScope.launch {
            val soundEnabled = repository.getRoutineEndSoundEnabled().first()
            val soundId = repository.getRoutineEndSoundId().first()
            val ttsOn = repository.getRoutineTtsEnabled().first()
            if (soundEnabled) SoundPlayer.playById(soundId)
            if (ttsOn) TtsPlayer.speak("Routine complete. Well done.")
        }
    }

    fun linkSessionToHabit(habitId: Long) {
        viewModelScope.launch {
            val user = repository.getResolvedActiveUser().first() ?: return@launch
            val habit = habitsForLinking.value.firstOrNull { it.id == habitId } ?: return@launch
            repository.saveDailyHabitEntry(
                userId = user.id,
                date = LocalDate.now(),
                habitId = habitId,
                type = habit.type,
                booleanValue = true,
                numericValue = when (habit.type) {
                    HabitType.COUNT, HabitType.DURATION, HabitType.TIMER, HabitType.NUMBER -> habit.targetValue ?: 1.0
                    else -> null
                }
            )
            linkedHabitName = habit.name
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
