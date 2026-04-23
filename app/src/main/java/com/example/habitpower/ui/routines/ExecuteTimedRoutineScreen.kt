package com.example.habitpower.ui.routines

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.execution.ExerciseInstructionBlock
import com.example.habitpower.ui.execution.ExerciseSpecsRow
import com.example.habitpower.ui.execution.NextUpCard
import com.example.habitpower.ui.execution.describeNextTimedStep
import com.example.habitpower.ui.execution.estimateTimedRoutineSeconds
import com.example.habitpower.ui.execution.formatExerciseTime
import com.example.habitpower.ui.exercises.ExerciseImage

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
    val ttsEnabled = viewModel.ttsEnabled
    val currentRound = viewModel.currentRound
    val totalRounds = viewModel.totalRounds
    val restTimeSeconds = routine?.restTimeSeconds ?: 0
    val nextTimedStep = describeNextTimedStep(
        exercises = exercises,
        currentExerciseIndex = viewModel.currentExerciseIndex,
        currentRound = currentRound,
        totalRounds = totalRounds
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTts() }) {
                        Icon(
                            imageVector = if (ttsEnabled) {
                                Icons.Default.RecordVoiceOver
                            } else {
                                Icons.Default.VoiceOverOff
                            },
                            contentDescription = if (ttsEnabled) "TTS On" else "TTS Off",
                            tint = if (ttsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
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
                    totalRounds = totalRounds,
                    restTimeSeconds = restTimeSeconds,
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
                    currentRound = currentRound,
                    totalRounds = totalRounds,
                    nextStepLabel = nextTimedStep,
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
                    nextStepLabel = nextTimedStep,
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
    totalRounds: Int,
    restTimeSeconds: Int,
    onStart: () -> Unit
) {
    val firstExercise = exercises.firstOrNull()
    val estimatedTime = estimateTimedRoutineSeconds(exercises, totalRounds, restTimeSeconds)

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
                buildString {
                    append("${exercises.size} exercise${if (exercises.size != 1) "s" else ""}")
                    if (totalRounds > 1) append(" - $totalRounds rounds")
                    if (restTimeSeconds > 0) append(" - ${restTimeSeconds}s rest")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExerciseSpecsRow(
                exercise = firstExercise ?: Exercise(name = "", description = "", imageUri = null, targetDurationSeconds = null, targetReps = null, targetSets = null),
                extraSpecs = buildList {
                    if (estimatedTime > 0) add("Est." to formatExerciseTime(estimatedTime))
                }
            )

            firstExercise?.let {
                ExerciseImage(
                    imageUri = it.imageUri,
                    contentDescription = it.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    exerciseName = it.name,
                    category = it.category,
                    detailLabel = it.description.takeIf { text -> text.isNotBlank() },
                    contentScale = ContentScale.Fit
                )
            }

            NextUpCard(
                title = "Execution style",
                body = "This routine auto-advances and keeps the clock moving, which is great for interval work and guided sessions."
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                exercises.forEachIndexed { index, exercise ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${index + 1}. ${exercise.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (exercise.description.isNotBlank()) {
                            Text(
                                text = exercise.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        exercise.targetDurationSeconds?.let {
                            Text(
                                text = formatExerciseTime(it),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
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
    currentRound: Int,
    totalRounds: Int,
    nextStepLabel: String?,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val positionLabel = if (totalRounds > 1) {
                "Round $currentRound/$totalRounds - Exercise $position/$totalExercises"
            } else {
                "Exercise $position/$totalExercises"
            }
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            ExerciseImage(
                imageUri = exercise.imageUri,
                contentDescription = exercise.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                exerciseName = exercise.name,
                category = exercise.category,
                detailLabel = exercise.description.takeIf { it.isNotBlank() },
                contentScale = ContentScale.Fit
            )

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (exercise.description.isNotBlank()) {
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExerciseSpecsRow(
                exercise = exercise,
                extraSpecs = listOf("Remaining" to formatExerciseTime(timeRemaining))
            )

            ExerciseInstructionBlock(exercise = exercise)

            nextStepLabel?.let {
                NextUpCard(
                    title = "Next step",
                    body = it
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = formatExerciseTime(timeRemaining),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp
            )

            LinearProgressIndicator(
                progress = {
                    (totalDuration - timeRemaining).toFloat() /
                        totalDuration.toFloat().coerceAtLeast(1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Text(
                text = if (isRunning) "RUNNING" else "PAUSED",
                style = MaterialTheme.typography.labelMedium,
                color = if (isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onPlayPause, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }
                Button(onClick = onSkip, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("Skip")
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Text("End Routine")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RestPhaseUI(
    modifier: Modifier = Modifier,
    timeRemaining: Int,
    isRunning: Boolean,
    nextStepLabel: String?,
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
                modifier = Modifier.size(130.dp),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "REST",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = formatExerciseTime(timeRemaining),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (isRunning) "Recover and reset" else "Paused",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            Spacer(Modifier.height(16.dp))

            nextStepLabel?.let {
                NextUpCard(
                    title = "Coming up next",
                    body = it
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onPlayPause, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }
                Button(onClick = onSkip, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("Skip Rest")
                }
            }
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Text("End Routine")
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Routine Completed!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Great job! Keep up the excellent work.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        Button(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Done", fontSize = 18.sp)
        }
    }
}
