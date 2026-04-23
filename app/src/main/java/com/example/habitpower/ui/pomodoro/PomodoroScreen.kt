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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.PomodoroSettings
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.PomodoroSession
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.util.SoundPlayer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    // Settings dialog
    if (state.showSettings) {
        PomodoroSettingsDialog(
            current = state.settings,
            onSave = { viewModel.saveSettings(it) },
            onDismiss = { viewModel.dismissSettings() }
        )
    }

    // Celebration dialog (after each completed focus interval)
    if (state.showCelebration) {
        val cycleNum = state.cyclesCompleted
        if (state.isFreeSession) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCelebration() },
                title = { Text("Focus Complete! ⚡  (Round $cycleNum)") },
                text = { Text("${state.settings.focusMinutes} minutes of focused work logged. Link this session to a habit?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.dismissCelebration()
                        state.sessionToLink?.let { viewModel.openLinkDialog(it) }
                    }) {
                        Text("Link to Habit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCelebration() }) {
                        Text("Later")
                    }
                }
            )
        } else {
            val habit = state.selectedHabit
            AlertDialog(
                onDismissRequest = { viewModel.dismissCelebration() },
                title = { Text("Focus Complete! 🍅  (Round $cycleNum)") },
                text = {
                    Text(
                        if (habit != null) "Session $cycleNum logged for ${habit.name}."
                        else "Great focus session!"
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissCelebration() }) {
                        Text("Keep Going")
                    }
                }
            )
        }
    }

    // Link dialog
    if (state.showLinkDialog && state.sessionToLink != null) {
        LinkSessionDialog(
            pomodoroHabits = state.pomodoroHabits.filter { it.type == HabitType.POMODORO },
            onLink = { habit -> viewModel.linkSession(state.sessionToLink!!, habit) },
            onDismiss = { viewModel.dismissLinkDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus") },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSound() }
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
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        enabled = !state.isRunning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pomodoro settings",
                            tint = if (!state.isRunning)
                                MaterialTheme.colorScheme.onSurface
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
            if (state.isInSession) {
                RunningSessionContent(state = state, viewModel = viewModel)
            } else {
                IdleContent(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.RunningSessionContent(state: PomodoroState, viewModel: PomodoroViewModel) {
    if (state.isRunning) KeepScreenOn()

    Spacer(modifier = Modifier.weight(1f))

    // Phase + cycle indicator
    val phaseLabel = when (state.phase) {
        PomodoroPhase.FOCUS -> "FOCUS · Round ${state.cyclesCompleted + 1} of ${state.settings.cyclesBeforeLongBreak}"
        PomodoroPhase.SHORT_BREAK -> "SHORT BREAK  ·  ${state.settings.shortBreakMinutes} min"
        PomodoroPhase.LONG_BREAK -> "LONG BREAK  ·  ${state.settings.longBreakMinutes} min"
    }
    val phaseColor = when (state.phase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
        PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondary
    }
    Text(text = phaseLabel, style = MaterialTheme.typography.labelLarge, color = phaseColor)

    Spacer(modifier = Modifier.height(8.dp))

    // Session label
    val sessionLabel = if (state.isFreeSession) {
        "⚡ Free Session"
    } else {
        val typeIcon = if (state.selectedHabit?.type == HabitType.TIMER) "⏱" else "🍅"
        "$typeIcon ${state.selectedHabit?.name ?: ""}"
    }
    Text(
        text = sessionLabel,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Countdown — dimmed when paused
    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60
    Text(
        text = String.format(java.util.Locale.US, "%02d:%02d", mins, secs),
        fontSize = 80.sp,
        fontWeight = FontWeight.Bold,
        color = if (state.isRunning) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    )

    Spacer(modifier = Modifier.height(64.dp))

    // Controls differ by running vs. paused
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.isRunning) {
            // Active countdown controls
            if (state.phase == PomodoroPhase.FOCUS) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { viewModel.addMinute() }) { Text("+1 min") }
                    Button(onClick = { viewModel.finishFocusEarly() }) { Text("Complete Focus") }
                }
            } else {
                Button(onClick = { viewModel.skipBreak() }) { Text("Skip Break") }
            }
        } else {
            // Paused between phases — require explicit tap to start
            val startLabel = when (state.phase) {
                PomodoroPhase.FOCUS -> "Start Round ${state.cyclesCompleted + 1}"
                PomodoroPhase.SHORT_BREAK -> "Start Short Break"
                PomodoroPhase.LONG_BREAK -> "Start Long Break"
            }
            Button(
                onClick = { viewModel.startCurrentPhase() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = phaseColor
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(startLabel)
            }
            if (state.phase != PomodoroPhase.FOCUS) {
                TextButton(onClick = { viewModel.skipBreak() }) {
                    Text("Skip Break", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(
            onClick = { viewModel.cancelFullSession() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("End Session")
        }
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
private fun IdleContent(state: PomodoroState, viewModel: PomodoroViewModel) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Evening nudge banner
        if (state.unlinkedSessions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 You have ${state.unlinkedSessions.size} unlinked session(s) today — link them to a habit or they'll clear at midnight.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Settings summary
        val s = state.settings
        Text(
            text = "⏱ ${s.focusMinutes} min focus · ${s.shortBreakMinutes} min break · ${s.longBreakMinutes} min long break · ${s.cyclesBeforeLongBreak} cycles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Free session card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚡ Start Free Focus Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No habit needed — link sessions to a habit after",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.startFreeSession() }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Free")
                }
            }
        }

        // Habit-linked section
        if (state.pomodoroHabits.isEmpty()) {
            Text(
                text = "No active Pomodoro or Timer habits found for today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Or focus on a habit:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            state.pomodoroHabits.forEach { habit ->
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
                        Column(modifier = Modifier.weight(1f)) {
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
                                onClick = { viewModel.startHabitSession() },
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

        // Unlinked sessions section
        if (state.unlinkedSessions.isNotEmpty()) {
            Text(
                text = "Unlinked Sessions (${state.unlinkedSessions.size})",
                style = MaterialTheme.typography.titleMedium
            )
            state.unlinkedSessions.forEach { session ->
                UnlinkedSessionCard(
                    session = session,
                    onLink = { viewModel.openLinkDialog(session) },
                    onDiscard = { viewModel.discardSession(session) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun UnlinkedSessionCard(
    session: PomodoroSession,
    onLink: () -> Unit,
    onDiscard: () -> Unit
) {
    val timeStr = remember(session.completedAt) {
        val ldt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(session.completedAt),
            ZoneId.systemDefault()
        )
        ldt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚡ ${session.durationMinutes} min  ·  completed at $timeStr",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onLink) {
                Text("Link")
            }
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Discard")
            }
        }
    }
}

@Composable
private fun LinkSessionDialog(
    pomodoroHabits: List<DailyHabitItem>,
    onLink: (DailyHabitItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Session to a Habit") },
        text = {
            if (pomodoroHabits.isEmpty()) {
                Text("No Pomodoro habits found. Create one in the Habits screen first.")
            } else {
                Column {
                    pomodoroHabits.forEach { habit ->
                        val current = habit.entryNumericValue?.toInt() ?: 0
                        val target = habit.targetValue?.toInt()?.toString() ?: "?"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLink(habit) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$current / $target cycles today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PomodoroSettingsDialog(
    current: PomodoroSettings,
    onSave: (PomodoroSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var focusText by remember { mutableStateOf(current.focusMinutes.toString()) }
    var shortBreakText by remember { mutableStateOf(current.shortBreakMinutes.toString()) }
    var longBreakText by remember { mutableStateOf(current.longBreakMinutes.toString()) }
    var cyclesText by remember { mutableStateOf(current.cyclesBeforeLongBreak.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pomodoro Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = focusText,
                    onValueChange = { focusText = it },
                    label = { Text("Focus duration (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = shortBreakText,
                    onValueChange = { shortBreakText = it },
                    label = { Text("Short break (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = longBreakText,
                    onValueChange = { longBreakText = it },
                    label = { Text("Long break (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = cyclesText,
                    onValueChange = { cyclesText = it },
                    label = { Text("Cycles before long break") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("e.g. 4 focus rounds then long break") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val focus = focusText.toIntOrNull()?.coerceIn(1, 120) ?: current.focusMinutes
                val short = shortBreakText.toIntOrNull()?.coerceIn(1, 60) ?: current.shortBreakMinutes
                val long = longBreakText.toIntOrNull()?.coerceIn(1, 60) ?: current.longBreakMinutes
                val cycles = cyclesText.toIntOrNull()?.coerceIn(1, 10) ?: current.cyclesBeforeLongBreak
                onSave(PomodoroSettings(focus, short, long, cycles))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
