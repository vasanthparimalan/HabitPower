package com.example.habitpower.ui.exercises

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.ExerciseLibraryRepository
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.ExerciseLibraryItem
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class AddEditExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository,
    val libraryRepository: ExerciseLibraryRepository
) : ViewModel() {

    private val exerciseId: Long? = savedStateHandle.get<String>("exerciseId")?.toLongOrNull()?.takeIf { it != -1L }

    var name by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var imageUri by mutableStateOf<String?>(null)
        private set
    var notes by mutableStateOf("")
        private set
    var instructions by mutableStateOf("")
        private set
    var tags by mutableStateOf("")
        private set
    var category by mutableStateOf(ExerciseCategory.STRENGTH)
        private set

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                repository.getExerciseById(exerciseId)?.let { exercise ->
                    name = exercise.name
                    description = exercise.description
                    imageUri = exercise.imageUri
                    notes = exercise.notes ?: ""
                    instructions = exercise.instructions ?: ""
                    tags = exercise.tags
                    category = exercise.category
                }
            }
        }
    }

    fun updateName(input: String) { name = input }
    fun updateDescription(input: String) { description = input }
    fun updateImageUri(input: String?) { imageUri = input }

    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val directory = File(context.filesDir, "exercise_images")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val mimeType = context.contentResolver.getType(uri)
                    val extensionFromMimeType = mimeType
                        ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                        ?.lowercase()
                    val extensionFromUri = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                        ?.lowercase()
                        ?.takeIf { it.isNotBlank() }
                    val extension = (extensionFromMimeType ?: extensionFromUri ?: "jpg")
                        .removePrefix(".")
                    val fileName = "${UUID.randomUUID()}.$extension"
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

    fun updateNotes(input: String) { notes = input }
    fun updateInstructions(input: String) { instructions = input }
    fun updateTags(input: String) { tags = input }
    fun updateCategory(cat: ExerciseCategory) { category = cat }

    fun prefillFromLibrary(item: ExerciseLibraryItem) {
        name = item.name
        description = item.primaryMuscle ?: ""
        instructions = item.instructions ?: ""
        imageUri = item.imageUri
        category = item.category
        if (tags.isBlank()) {
            tags = item.category.name.lowercase()
        }
    }

    fun saveExercise() {
        if (name.isBlank()) return

        val exercise = Exercise(
            id = exerciseId ?: 0,
            name = name,
            description = description,
            imageUri = imageUri,
            notes = notes,
            instructions = instructions.ifBlank { null },
            tags = tags,
            category = category
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
