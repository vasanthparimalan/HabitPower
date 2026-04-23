package com.example.habitpower.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.LeafSectionItemCard
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoutineScreen(
    navigateBack: () -> Unit,
    viewModel: AddEditRoutineViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.addedExercises.isEmpty() && viewModel.name.isBlank()) "New Routine" else "Edit Routine") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { viewModel.saveRoutine(onSaved = navigateBack) }) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::updateName,
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth(),
                isError = viewModel.name.isBlank()
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Routine Type", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewModel.routineType == RoutineType.NORMAL,
                    onClick = { viewModel.updateRoutineType(RoutineType.NORMAL) },
                    label = { Text("Normal — Manual advance") },
                    leadingIcon = {
                        if (viewModel.routineType == RoutineType.NORMAL)
                            Icon(Icons.Default.Check, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = viewModel.routineType == RoutineType.TIMED,
                    onClick = { viewModel.updateRoutineType(RoutineType.TIMED) },
                    label = { Text("Timed — Auto-advance") },
                    leadingIcon = {
                        if (viewModel.routineType == RoutineType.TIMED)
                            Icon(Icons.Default.Check, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (viewModel.routineType == RoutineType.TIMED) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.restTimeSeconds,
                        onValueChange = viewModel::updateRestTimeSeconds,
                        label = { Text("Rest between exercises (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("e.g. 10 or 30") }
                    )
                    OutlinedTextField(
                        value = viewModel.repeatCount,
                        onValueChange = viewModel::updateRepeatCount,
                        label = { Text("Rounds") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("1 = single pass") }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Exercises  (${viewModel.addedExercises.size})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showAddExerciseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise to Routine")
                }
            }

            if (viewModel.addedExercises.isEmpty()) {
                Text(
                    text = "No exercises added yet. Tap + to add from your exercise library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val lazyListState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    viewModel.moveExercise(from.index, to.index)
                }
                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(viewModel.addedExercises, key = { _, ex -> ex.id }) { index, exercise ->
                        ReorderableItem(reorderState, key = exercise.id) {
                            RoutineExerciseItem(
                                exercise = exercise,
                                position = index + 1,
                                onRemove = { viewModel.removeExercise(exercise) },
                                dragHandle = {
                                    IconButton(
                                        modifier = Modifier.draggableHandle(),
                                        onClick = {}
                                    ) {
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExercisePickerDialog(
            viewModel = viewModel,
            onDismiss = { showAddExerciseDialog = false }
        )
    }
}

@Composable
private fun ExercisePickerDialog(
    viewModel: AddEditRoutineViewModel,
    onDismiss: () -> Unit
) {
    val allExercises by viewModel.allExercises.collectAsState()
    var exerciseSearchQuery by rememberSaveable { mutableStateOf("") }

    val addedIds = viewModel.addedExercises.map { it.id }.toSet()

    val filteredExercises = allExercises
        .filter { exercise ->
            val query = exerciseSearchQuery.trim()
            query.isBlank() ||
                exercise.name.contains(query, ignoreCase = true) ||
                exercise.tags.contains(query, ignoreCase = true)
        }
        .sortedBy { it.name.lowercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercises") },
        text = {
            if (allExercises.isEmpty()) {
                Text("No exercises found. Create exercises in the Exercises screen first.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exerciseSearchQuery,
                        onValueChange = { exerciseSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search by name or tag") },
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(filteredExercises, key = { it.id }) { exercise ->
                            val alreadyAdded = exercise.id in addedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !alreadyAdded) {
                                        // Stay open — don't dismiss. "Done" closes.
                                        viewModel.addExercise(exercise)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        color = if (alreadyAdded)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = exercise.category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (exercise.tags.isNotBlank()) {
                                    Text(
                                        text = exercise.tags,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                if (alreadyAdded) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done (${viewModel.addedExercises.size})") }
        }
    )
}

@Composable
fun RoutineExerciseItem(
    exercise: Exercise,
    position: Int,
    onRemove: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dragHandle?.invoke()
        LeafSectionItemCard(
            modifier = Modifier.weight(1f),
            title = "$position. ${exercise.name}",
            subtitle = exercise.description.takeIf { it.isNotBlank() },
            attributes = buildList {
                exercise.targetSets?.let { add("Sets" to it.toString()) }
                exercise.targetReps?.let { add("Reps" to it.toString()) }
                exercise.targetDurationSeconds?.let { add("Duration" to "${it}s") }
            },
            trailingActions = {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove ${exercise.name}")
                }
            }
        )
    }
}
