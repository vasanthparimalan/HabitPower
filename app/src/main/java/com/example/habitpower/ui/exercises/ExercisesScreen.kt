package com.example.habitpower.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.theme.LeafSectionItemCard
import com.example.habitpower.util.ExercisePackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    onNavigate: (String) -> Unit,
    viewModel: ExercisesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val exercises by viewModel.exercises.collectAsState()

    val grouped = ExerciseCategory.entries.associateWith { cat ->
        exercises.filter { it.category == cat }
    }

    val expanded = remember {
        mutableStateMapOf<ExerciseCategory, Boolean>().apply {
            ExerciseCategory.entries.forEach { category -> this[category] = true }
        }
    }
    fun isExpanded(cat: ExerciseCategory) = expanded[cat] == true

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.LibraryBrowse.route) }) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = "Browse Library")
                    }
                    IconButton(onClick = { onNavigate(Screen.ImportPack.route) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import Pack")
                    }
                    if (exercises.isNotEmpty()) {
                        IconButton(onClick = { ExercisePackManager.share(context, exercises) }) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export Pack")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigate(Screen.AddEditExercise.createRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item("exercise_library_intro") {
                Text(
                    text = if (exercises.isEmpty()) {
                        "Your exercise library is ready. The four sections stay visible so you can add or spot the right kind of movement quickly."
                    } else {
                        "Collapse sections to jump between strength, yoga, stretching, and cardio faster."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            ExerciseCategory.entries.forEach { cat ->
                val list = grouped.getValue(cat)
                item(key = cat.name + "_header") {
                    CategorySectionHeader(
                        title = cat.displayName,
                        count = list.size,
                        isExpanded = isExpanded(cat),
                        onToggle = { expanded[cat] = !isExpanded(cat) }
                    )
                }
                if (isExpanded(cat)) {
                    if (list.isEmpty()) {
                        item(key = cat.name + "_empty") {
                            Text(
                                text = "No ${cat.displayName.lowercase()} exercises yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(list, key = { it.id }) { exercise ->
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                ExerciseItem(
                                    exercise = exercise,
                                    onEditClick = { onNavigate(Screen.AddEditExercise.createRoute(exercise.id)) },
                                    onDeleteClick = { viewModel.deleteExercise(exercise) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val attributes = buildList {
        exercise.targetSets?.let { add("Sets" to it.toString()) }
        exercise.targetReps?.let { add("Reps" to it.toString()) }
        exercise.targetDurationSeconds?.let { add("Duration" to "${it}s") }
        val tagsSummary = exercise.tags.split(',').map { it.trim() }.filter { it.isNotBlank() }.take(2).joinToString(", ")
        if (tagsSummary.isNotBlank()) add("Tags" to tagsSummary)
    }

    LeafSectionItemCard(
        title = exercise.name,
        subtitle = exercise.description,
        attributes = attributes,
        leading = {
            ExerciseImage(
                imageUri = exercise.imageUri,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = onEditClick),
                exerciseName = exercise.name,
                category = exercise.category,
                detailLabel = exercise.description.takeIf { it.isNotBlank() },
                iconSize = 32.dp
            )
        },
        trailingActions = {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}
