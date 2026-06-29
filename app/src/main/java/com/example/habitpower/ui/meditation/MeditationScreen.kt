package com.example.habitpower.ui.meditation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.MeditationPreset
import com.example.habitpower.data.model.MeditationPresets
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.util.SoundPlayer
import kotlinx.coroutines.delay

private sealed interface MeditationUiState {
    data object Picker : MeditationUiState
    data class Active(val preset: MeditationPreset, val running: Boolean) : MeditationUiState
    data class Done(val preset: MeditationPreset) : MeditationUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    navigateBack: () -> Unit,
    onSessionComplete: () -> Unit = navigateBack,
    viewModel: MeditationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var bellAtStart by rememberSaveable { mutableStateOf(true) }
    var bellAtEnd by rememberSaveable { mutableStateOf(true) }
    var intervalMinutes by rememberSaveable { mutableIntStateOf(0) }
    var customDurationMinutes by rememberSaveable { mutableIntStateOf(10) }

    fun makeCustomPreset(minutes: Int) = MeditationPreset(
        name = "Free Session",
        description = "$minutes min · custom timer",
        durationSeconds = minutes * 60,
        guidanceText = "Sit comfortably. Let the timer hold the time."
    )

    var uiState by rememberSaveable(
        stateSaver = meditationStateSaver { makeCustomPreset(customDurationMinutes) }
    ) {
        mutableStateOf<MeditationUiState>(MeditationUiState.Picker)
    }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState) {
        val active = uiState as? MeditationUiState.Active ?: return@LaunchedEffect
        if (!active.running) return@LaunchedEffect

        val isFreshStart = remainingSeconds == active.preset.durationSeconds
        if (bellAtStart && isFreshStart) SoundPlayer.playById("short_beep", 75)

        var elapsed = active.preset.durationSeconds - remainingSeconds
        while (remainingSeconds > 0) {
            delay(1_000L)
            val stillActive = uiState as? MeditationUiState.Active
            if (stillActive == null || !stillActive.running) return@LaunchedEffect
            remainingSeconds -= 1
            elapsed += 1
            if (intervalMinutes > 0 && elapsed % (intervalMinutes * 60) == 0 && remainingSeconds > 0) {
                SoundPlayer.playById("short_beep", 60)
            }
        }

        if (bellAtEnd) SoundPlayer.playSessionEnd()
        uiState = MeditationUiState.Done((uiState as MeditationUiState.Active).preset)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meditation") },
                navigationIcon = {
                    IconButton(onClick = {
                        uiState = MeditationUiState.Picker
                        navigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { state ->
            when (state) {
                is MeditationUiState.Picker -> PresetPicker(
                    onSelect = { preset ->
                        remainingSeconds = preset.durationSeconds
                        uiState = MeditationUiState.Active(preset, running = false)
                    },
                    onStartCustom = { minutes ->
                        customDurationMinutes = minutes
                        val preset = makeCustomPreset(minutes)
                        remainingSeconds = preset.durationSeconds
                        uiState = MeditationUiState.Active(preset, running = false)
                    },
                    bellAtStart = bellAtStart,
                    bellAtEnd = bellAtEnd,
                    intervalMinutes = intervalMinutes,
                    onBellAtStartChange = { bellAtStart = it },
                    onBellAtEndChange = { bellAtEnd = it },
                    onIntervalChange = { intervalMinutes = it }
                )

                is MeditationUiState.Active -> ActiveTimer(
                    preset = state.preset,
                    remainingSeconds = remainingSeconds,
                    running = state.running,
                    onToggleRunning = { uiState = state.copy(running = !state.running) },
                    onCancel = {
                        remainingSeconds = 0
                        uiState = MeditationUiState.Picker
                    }
                )

                is MeditationUiState.Done -> {
                    val habitsForLinking by viewModel.habitsForLinking.collectAsStateWithLifecycle()
                    CompletionCard(
                        preset = state.preset,
                        habitsForLinking = habitsForLinking,
                        onLinkToHabit = { viewModel.linkSessionToHabit(it) },
                        onDone = {
                            viewModel.logSession(state.preset.name, state.preset.durationSeconds.toLong())
                            uiState = MeditationUiState.Picker
                            onSessionComplete()
                        },
                        onRepeat = {
                            remainingSeconds = state.preset.durationSeconds
                            uiState = MeditationUiState.Active(state.preset, running = false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetPicker(
    onSelect: (MeditationPreset) -> Unit,
    onStartCustom: (Int) -> Unit,
    bellAtStart: Boolean,
    bellAtEnd: Boolean,
    intervalMinutes: Int,
    onBellAtStartChange: (Boolean) -> Unit,
    onBellAtEndChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    val durationOptions = listOf(5, 10, 15, 20, 30, 45, 60)
    val intervalOptions = listOf(0 to "Off", 5 to "5 min", 10 to "10 min", 15 to "15 min")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Free Session card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Free Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Pick a duration and begin immediately",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { mins ->
                            FilterChip(
                                selected = false,
                                onClick = { onStartCustom(mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                    var customDurationText by remember { mutableStateOf("") }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = customDurationText,
                            onValueChange = { customDurationText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("Custom (min)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        Button(
                            onClick = {
                                val mins = customDurationText.toIntOrNull()
                                if (mins != null && mins in 1..180) onStartCustom(mins)
                            },
                            enabled = customDurationText.toIntOrNull()?.let { it in 1..180 } == true
                        ) { Text("Start") }
                    }
                }
            }
        }

        // Preset sessions
        item {
            Text(
                "GUIDED SESSIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(MeditationPresets.all) { preset ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = { onSelect(preset) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatDuration(preset.durationSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sound settings
        item {
            Text(
                "SOUND SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bell at start", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = bellAtStart, onCheckedChange = onBellAtStartChange)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bell at end", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = bellAtEnd, onCheckedChange = onBellAtEndChange)
                    }
                    HorizontalDivider()
                    Text("Interval bell", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervalOptions.forEach { (mins, label) ->
                            FilterChip(
                                selected = intervalMinutes == mins,
                                onClick = { onIntervalChange(mins) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ActiveTimer(
    preset: MeditationPreset,
    remainingSeconds: Int,
    running: Boolean,
    onToggleRunning: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.guidanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = formatDuration(remainingSeconds),
            fontSize = 80.sp,
            fontWeight = FontWeight.Light,
            color = if (running) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onToggleRunning, modifier = Modifier.fillMaxWidth(0.6f)) {
                Icon(
                    imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (running) "Pause" else if (remainingSeconds == preset.durationSeconds) "Begin" else "Resume")
            }
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun CompletionCard(
    preset: MeditationPreset,
    habitsForLinking: List<HabitDefinition>,
    onLinkToHabit: (Long) -> Unit,
    onDone: () -> Unit,
    onRepeat: () -> Unit
) {
    var showHabitPicker by remember { mutableStateOf(false) }
    var linkedHabitName by remember { mutableStateOf<String?>(null) }

    if (showHabitPicker) {
        AlertDialog(
            onDismissRequest = { showHabitPicker = false },
            title = { Text("Link to a habit") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(habitsForLinking, key = { it.id }) { habit ->
                        TextButton(
                            onClick = {
                                onLinkToHabit(habit.id)
                                linkedHabitName = habit.name
                                showHabitPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(habit.name, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHabitPicker = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔔", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Session complete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${preset.name} · ${formatDuration(preset.durationSeconds)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (linkedHabitName != null) {
            Text(
                text = "Linked to “$linkedHabitName”",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else if (habitsForLinking.isNotEmpty()) {
            TextButton(onClick = { showHabitPicker = true }) {
                Text("Link to a habit →")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onRepeat, modifier = Modifier.fillMaxWidth()) {
            Text("Sit again")
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", m, s)
}

private fun meditationStateSaver(makeCustomPreset: () -> MeditationPreset) =
    Saver<MeditationUiState, Int>(
        save = { state ->
            when (state) {
                is MeditationUiState.Picker -> 0
                is MeditationUiState.Active -> {
                    val idx = MeditationPresets.all.indexOf(state.preset)
                    if (idx >= 0) idx + 100 + if (state.running) 1000 else 0
                    else if (state.running) 6000 else 5000
                }
                is MeditationUiState.Done -> {
                    val idx = MeditationPresets.all.indexOf(state.preset)
                    if (idx >= 0) idx + 200 else 7000
                }
            }
        },
        restore = { code ->
            when {
                code == 0 -> MeditationUiState.Picker
                code >= 7000 -> MeditationUiState.Done(makeCustomPreset())
                code >= 6000 -> MeditationUiState.Active(makeCustomPreset(), running = false)
                code >= 5000 -> MeditationUiState.Active(makeCustomPreset(), running = false)
                code >= 1100 -> MeditationUiState.Active(
                    MeditationPresets.all.getOrElse(code - 1100) { MeditationPresets.all.first() },
                    running = false
                )
                code >= 1000 -> MeditationUiState.Active(
                    MeditationPresets.all.getOrElse(code - 1000) { MeditationPresets.all.first() },
                    running = false
                )
                code >= 200 -> MeditationUiState.Done(
                    MeditationPresets.all.getOrElse(code - 200) { MeditationPresets.all.first() }
                )
                code >= 100 -> MeditationUiState.Active(
                    MeditationPresets.all.getOrElse(code - 100) { MeditationPresets.all.first() },
                    running = false
                )
                else -> MeditationUiState.Picker
            }
        }
    )
