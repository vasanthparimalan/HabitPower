package com.example.habitpower.ui.execution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRunnerScreen(
    navigateBack: () -> Unit,
    viewModel: WorkoutRunnerViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    if (viewModel.isWorkoutComplete) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Workout Complete!", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = navigateBack) {
                Text("Back to Routines")
            }
        }
        return
    }

    val exercise = viewModel.currentExercise ?: return

    Scaffold(
        topBar = {
            // Hiding TopBar to give more space as requested or keeping it?
            // User said "Exercise name on top left". System TopBar might duplicate this or waste space.
            // Let's keep system top bar for "Back" navigation but maybe make it small or transparent?
            // Actually, the user requirement "Exercise name on top left" suggests it replaces the title or is just below it.
            // Let's keep standard TopBar for consistency but put the specifc details in the content.
            TopAppBar(title = { Text("Workout") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Row: Name and Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatTime(viewModel.timerSeconds),
                    style = MaterialTheme.typography.displaySmall
                )
            }

            // Subheader: Sets and Targets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (exercise.targetSets != null) {
                    Text(
                        text = "Sets: ${exercise.targetSets}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Show Target Reps or Duration here as well
                if (exercise.targetReps != null) {
                    Text(
                        text = "Reps: ${exercise.targetReps}",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else if (exercise.targetDurationSeconds != null) {
                    Text(
                        text = "Target: ${formatTime(exercise.targetDurationSeconds)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Image Area - Takes remaining space (approx 75% visually if buttons are small)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                    .padding(4.dp) // border effect
            ) {
                if (exercise.imageUri != null) {
                    AsyncImage(
                        model = exercise.imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit // "fit the screen" usually means see the whole image
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Image")
                    }
                }
            }

            // Note if present
            if (!exercise.notes.isNullOrBlank()) {
                Text(
                    text = "Note: ${exercise.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Bottom Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        if (viewModel.isTimerRunning) Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Timer"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (viewModel.isTimerRunning) "Pause" else "Start Timer")
                }

                Button(
                    onClick = { viewModel.nextExercise() },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Next / Done")
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
