package com.example.habitpower.ui.routines

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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.ui.AppViewModelProvider
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecuteTimedRoutineScreen(
    navigateBack: () -> Unit,
    onRoutineComplete: () -> Unit,
    viewModel: TimedRoutineExecutorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val phase = viewModel.currentPhase
    val routine = viewModel.routine
    val exercises = viewModel.exercises
    val isRunning = viewModel.isRunning

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            phase is ExecutionPhase.Idle && exercises.isNotEmpty() -> {
                IdlePhaseUI(
                    modifier = Modifier.padding(innerPadding),
                    exercises = exercises,
                    onStart = { viewModel.startRoutine() }
                )
            }
            phase is ExecutionPhase.ExercisePhase -> {
                ExercisePhaseUI(
                    modifier = Modifier.padding(innerPadding),
                    exercise = phase.exercise,
                    position = phase.position + 1,
                    totalExercises = exercises.size,
                    timeRemaining = phase.timeRemaining,
                    totalDuration = phase.exercise.targetDurationSeconds ?: 60,
                    isRunning = isRunning,
                    onPlayPause = {
                        if (isRunning) viewModel.pauseRoutine() else viewModel.resumeRoutine()
                    },
                    onSkip = { viewModel.skipToNextExercise() },
                    onFinish = { viewModel.finishRoutine() }
                )
            }
            phase is ExecutionPhase.RestPhase -> {
                RestPhaseUI(
                    modifier = Modifier.padding(innerPadding),
                    timeRemaining = phase.timeRemaining,
                    isRunning = isRunning,
                    onPlayPause = {
                        if (isRunning) viewModel.pauseRoutine() else viewModel.resumeRoutine()
                    },
                    onSkip = { viewModel.skipToNextExercise() },
                    onFinish = { viewModel.finishRoutine() }
                )
            }
            phase is ExecutionPhase.Completed -> {
                CompletedPhaseUI(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateHome = onRoutineComplete
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No exercises in routine")
                }
            }
        }
    }
}

@Composable
private fun IdlePhaseUI(
    modifier: Modifier = Modifier,
    exercises: List<Exercise>,
    onStart: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ready to Start?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${exercises.size} exercises",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Start Routine", fontSize = 18.sp)
        }
    }
}

@Composable
private fun ExercisePhaseUI(
    modifier: Modifier = Modifier,
    exercise: Exercise,
    position: Int,
    totalExercises: Int,
    timeRemaining: Int,
    totalDuration: Int,
    isRunning: Boolean,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Exercise info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Exercise $position / $totalExercises",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Exercise image if available
            if (!exercise.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = exercise.imageUri,
                    contentDescription = exercise.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (exercise.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Timer and progress
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Countdown timer
            Text(
                text = String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (totalDuration - timeRemaining).toFloat() / totalDuration.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status
            Text(
                text = if (isRunning) "RUNNING" else "PAUSED",
                style = MaterialTheme.typography.labelMedium,
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPlayPause,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (isRunning) "Pause" else "Resume")
            }

            Button(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Skip")
            }
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("End Routine")
        }
    }
}

@Composable
private fun RestPhaseUI(
    modifier: Modifier = Modifier,
    timeRemaining: Int,
    isRunning: Boolean,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(150.dp),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REST",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Countdown timer
            Text(
                text = String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRunning) "Get Ready..." else "PAUSED",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPlayPause,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (isRunning) "Pause" else "Resume")
            }

            Button(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun CompletedPhaseUI(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(150.dp),
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Routine Completed!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Great job! Keep up the excellent work.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Done", fontSize = 18.sp)
        }
    }
}
