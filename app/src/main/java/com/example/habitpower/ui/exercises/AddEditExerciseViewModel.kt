package com.example.habitpower.ui.exercises

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class AddEditExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val exerciseId: Long? = savedStateHandle.get<String>("exerciseId")?.toLongOrNull()?.takeIf { it != -1L }

    var name by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var imageUri by mutableStateOf<String?>(null)
        private set
    var targetSets by mutableStateOf("")
        private set

    var targetDuration by mutableStateOf("")
        private set
    var targetReps by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set

    // Helper state to toggle UI between Reps and Duration
    var isTimeBased by mutableStateOf(false)
        private set

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                repository.getExerciseById(exerciseId)?.let { exercise ->
                    name = exercise.name
                    description = exercise.description
                    imageUri = exercise.imageUri
                    targetSets = exercise.targetSets?.toString() ?: ""

                    // Determine initial mode based on data
                    if (exercise.targetDurationSeconds != null) {
                        isTimeBased = true
                        targetDuration = exercise.targetDurationSeconds.toString()
                        targetReps = ""
                    } else {
                        isTimeBased = false
                        targetReps = exercise.targetReps?.toString() ?: ""
                        targetDuration = ""
                    }

                    notes = exercise.notes ?: ""
                }
            }
        }
    }

    fun updateName(input: String) { name = input }
    fun updateDescription(input: String) { description = input }
    fun updateImageUri(input: String?) { imageUri = input }
    fun updateTargetSets(input: String) { targetSets = input }

    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val directory = File(context.filesDir, "exercise_images")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val fileName = "${UUID.randomUUID()}.jpg"
                    val file = File(directory, fileName)
                    val outputStream = FileOutputStream(file)

                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    imageUri = file.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error (optional: expose error state)
            }
        }
    }

    fun setTimeBasedMode(timeBased: Boolean) {
        isTimeBased = timeBased
        if (timeBased) {
            targetReps = ""
        } else {
            targetDuration = ""
        }
    }

    fun updateTargetDuration(input: String) { if (isTimeBased) targetDuration = input }

    fun updateTargetReps(input: String) { if (!isTimeBased) targetReps = input }

    fun updateNotes(input: String) { notes = input }

    fun saveExercise() {
        if (name.isBlank()) return

        val exercise = Exercise(
            id = exerciseId ?: 0,
            name = name,
            description = description,
            imageUri = imageUri,
            targetSets = targetSets.toIntOrNull(),
            targetDurationSeconds = if (isTimeBased) targetDuration.toIntOrNull() else null,
            targetReps = if (!isTimeBased) targetReps.toIntOrNull() else null,
            notes = notes
        )

        viewModelScope.launch {
            if (exerciseId == null) {
                repository.insertExercise(exercise)
            } else {
                repository.updateExercise(exercise)
            }
        }
    }
}
