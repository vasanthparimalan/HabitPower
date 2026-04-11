package com.example.habitpower.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.util.SoundPlayer

@Composable
fun KeepScreenOn() {
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    // Play sound when soundTrigger changes to a non-zero value
    LaunchedEffect(state.soundTrigger) {
        if (state.soundTrigger != 0L) {
            SoundPlayer.playById(state.selectedSoundId)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkResumeState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (state.showCelebration) {
        val isTimer = state.selectedHabit?.type == HabitType.TIMER
        AlertDialog(
            onDismissRequest = { viewModel.dismissCelebration() },
            title = { Text(if (isTimer) "Timer Complete! ✅" else "Session Completed! 🎉") },
            text = {
                Text(
                    if (isTimer) {
                        "Great focus! Your timer habit has been marked as done."
                    } else {
                        "Great job staying focused. Your Pomodoro session has been logged."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissCelebration() }) {
                    Text("Awesome")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus") },
                actions = {
                    // Speaker toggle — works for both Pomodoro and Timer
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier
                    ) {
                        Icon(
                            imageVector = if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (state.soundEnabled) "Sound on" else "Sound off",
                            tint = if (state.soundEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.pomodoroHabits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No active Pomodoro or Timer habits found for today.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.isRunning) {
                KeepScreenOn()

                Spacer(modifier = Modifier.weight(1f))

                state.selectedHabit?.let { habit ->
                    val typeLabel = if (habit.type == HabitType.TIMER) "⏱ Timer" else "🍅 Pomodoro"
                    Text(
                        text = "$typeLabel: ${habit.name}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                val mins = state.remainingSeconds / 60
                val secs = state.remainingSeconds % 60
                val timeString = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                Text(
                    text = timeString,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(64.dp))

                OutlinedButton(
                    onClick = { viewModel.cancelSession() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Session")
                }

                Spacer(modifier = Modifier.weight(1f))
            } else {
                Text(
                    text = "Select a task to focus on:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.pomodoroHabits) { habit ->
                        val isSelected = state.selectedHabit?.habitId == habit.habitId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectHabit(habit) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (habit.type == HabitType.TIMER) {
                                        val durationMins = habit.targetValue?.toInt() ?: 25
                                        val isDone = habit.entryNumericValue != null
                                        Text(
                                            text = "⏱ ${durationMins} min timer  •  ${if (isDone) "✅ Done today" else "Not done yet"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    } else {
                                        val current = habit.entryNumericValue?.toInt() ?: 0
                                        val target = habit.targetValue?.toInt()?.toString() ?: "?"
                                        Text(
                                            text = "🍅 Completed: $current / $target cycles",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (isSelected) {
                                    IconButton(
                                        onClick = { viewModel.startSession() },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
