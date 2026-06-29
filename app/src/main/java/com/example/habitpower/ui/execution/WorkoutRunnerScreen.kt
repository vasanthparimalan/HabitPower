package com.example.habitpower.ui.execution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.RoutineExerciseWithDetails
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.exercises.ExerciseImage

@Composable
private fun HabitLinkPickerDialog(
    habits: List<HabitDefinition>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link to a habit") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(habits, key = { it.id }) { habit ->
                    TextButton(
                        onClick = { onSelect(habit.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(habit.name, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRunnerScreen(
    navigateBack: () -> Unit,
    onRoutineComplete: () -> Unit,
    viewModel: WorkoutRunnerViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val habitsForLinking by viewModel.habitsForLinking.collectAsState()
    var showHabitPicker by remember { mutableStateOf(false) }

    if (viewModel.isWorkoutComplete) {
        if (showHabitPicker) {
            HabitLinkPickerDialog(
                habits = habitsForLinking,
                onDismiss = { showHabitPicker = false },
                onSelect = { habitId ->
                    viewModel.linkSessionToHabit(habitId)
                    showHabitPicker = false
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Routine Complete!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "All exercises done. Great work.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            if (viewModel.linkedHabitName != null) {
                Text(
                    text = "Linked to “${viewModel.linkedHabitName}”",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            } else if (habitsForLinking.isNotEmpty()) {
                TextButton(onClick = { showHabitPicker = true }) {
                    Text("Link to a habit →")
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = onRoutineComplete, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val total = viewModel.totalExercises
                    val current = viewModel.currentExerciseIndex + 1
                    Text(
                        if (!viewModel.isStarted) {
                            viewModel.routineName.ifBlank { "Routine" }
                        } else if (total > 0) {
                            "Exercise $current of $total"
                        } else {
                            "Routine"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!viewModel.isStarted) {
            if (viewModel.allExercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                NormalRoutineIdleScreen(
                    modifier = Modifier.padding(innerPadding),
                    exercises = viewModel.allExercises,
                    onStart = { viewModel.confirmStart() }
                )
            }
            return@Scaffold
        }

        val exerciseEntry = viewModel.currentExercise
        if (exerciseEntry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val exercise = exerciseEntry.exercise
        val crossRef = exerciseEntry.crossRef

        val totalExercises = viewModel.totalExercises.coerceAtLeast(1)
        val progress = (viewModel.currentExerciseIndex + 1).toFloat() / totalExercises.toFloat()
        val nextExerciseName = viewModel.allExercises
            .getOrNull(viewModel.currentExerciseIndex + 1)
            ?.exercise?.name

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (exercise.description.isNotBlank()) {
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ExerciseSpecsRow(
                    sets = crossRef.sets,
                    reps = crossRef.reps,
                    durationSeconds = crossRef.durationSeconds,
                    extraSpecs = buildList {
                        if (viewModel.isTimerRunning || viewModel.timerSeconds > 0) {
                            add("Timer" to formatExerciseTime(viewModel.timerSeconds))
                        }
                    }
                )

                ExerciseImage(
                    imageUri = exercise.imageUri,
                    contentDescription = exercise.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    exerciseName = exercise.name,
                    category = exercise.category,
                    detailLabel = exercise.description.takeIf { it.isNotBlank() },
                    contentScale = ContentScale.Fit
                )

                ExerciseInstructionBlock(exercise = exercise)

                nextExerciseName?.let {
                    NextUpCard(
                        title = "Up next",
                        body = it
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (viewModel.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (viewModel.isTimerRunning) "Pause" else "Start Timer")
                }
                Button(
                    onClick = { viewModel.nextExercise() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (viewModel.isLastExercise) "Finish" else "Next")
                }
            }
        }
    }
}

@Composable
private fun NormalRoutineIdleScreen(
    modifier: Modifier = Modifier,
    exercises: List<RoutineExerciseWithDetails>,
    onStart: () -> Unit
) {
    val firstEntry = exercises.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Ready to Start?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${exercises.size} exercise${if (exercises.size != 1) "s" else ""} - manual advance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            firstEntry?.let { entry ->
                ExerciseImage(
                    imageUri = entry.exercise.imageUri,
                    contentDescription = entry.exercise.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    exerciseName = entry.exercise.name,
                    category = entry.exercise.category,
                    detailLabel = entry.exercise.description.takeIf { it.isNotBlank() },
                    contentScale = ContentScale.Fit
                )
            }

            NextUpCard(
                title = "How this mode feels",
                body = "You decide when to move on, which makes this ideal for reps, weights, and form-focused work."
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                exercises.forEachIndexed { index, entry ->
                    val ex = entry.exercise
                    val cr = entry.crossRef
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${index + 1}. ${ex.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (ex.description.isNotBlank()) {
                            Text(
                                text = ex.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val plan = buildString {
                            cr.sets?.let { append("$it sets") }
                            cr.reps?.let {
                                if (isNotBlank()) append(" · ")
                                append("$it reps")
                            }
                            cr.durationSeconds?.let {
                                if (isNotBlank()) append(" · ")
                                append(formatExerciseTime(it))
                            }
                        }
                        if (plan.isNotBlank()) {
                            Text(
                                text = plan,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Start Routine", style = MaterialTheme.typography.titleMedium)
        }
    }
}
