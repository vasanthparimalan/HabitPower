package com.example.habitpower.ui.chant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.util.SoundPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChantScreen(
    navigateBack: () -> Unit,
    onSessionComplete: () -> Unit = navigateBack,
    viewModel: ChantViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.currentCount) {
        if (state.isInSession && state.currentCount > 0 && state.currentCount == state.targetCount / 2) {
            SoundPlayer.playById("short_beep", 50)
        }
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) SoundPlayer.playSessionEnd()
    }

    if (state.showAddDialog) {
        ChantFormDialog(
            title = "Add Custom Chant",
            initial = null,
            onDismiss = { viewModel.dismissAddDialog() },
            onSave = { name, text, tradition, count, audioUri ->
                viewModel.saveCustomChant(name, text, tradition, count, audioUri)
            }
        )
    }

    state.editingChant?.let { editing ->
        ChantFormDialog(
            title = "Edit Chant",
            initial = editing,
            onDismiss = { viewModel.dismissEditDialog() },
            onSave = { name, text, tradition, count, audioUri ->
                viewModel.updateCustomChant(name, text, tradition, count, audioUri)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chant Practice") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isInSession) viewModel.endSessionEarly()
                        navigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isInSession && !state.isComplete) {
                        IconButton(onClick = { viewModel.showAddDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "Add chant")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = when {
                state.isComplete -> "done"
                state.isInSession -> "session"
                state.selectedChant != null -> "confirm"
                else -> "picker"
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                "picker" -> ChantPicker(
                    chants = state.chants,
                    onSelect = { viewModel.selectChant(it) },
                    onEdit = { viewModel.showEditFor(it) },
                    onDelete = { viewModel.deleteCustomChant(it) }
                )

                "confirm" -> ChantConfirm(
                    chant = state.selectedChant!!,
                    targetCount = state.targetCount,
                    autoMode = state.autoMode,
                    autoIntervalSeconds = state.autoIntervalSeconds,
                    autoSoundId = state.autoSoundId,
                    onAutoModeChange = { viewModel.setAutoMode(it) },
                    onIntervalChange = { viewModel.setAutoInterval(it) },
                    onSoundChange = { viewModel.setAutoSound(it) },
                    onBegin = { viewModel.beginSession() },
                    onBack = { viewModel.selectChant(state.selectedChant!!.copy()) }
                )

                "session" -> ChantSession(
                    chant = state.selectedChant!!,
                    currentCount = state.currentCount,
                    targetCount = state.targetCount,
                    autoMode = state.autoMode,
                    isAudioMode = state.selectedChant?.audioUri != null,
                    audioUnavailable = state.audioUnavailable,
                    onTap = {
                        if (!state.autoMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.incrementCount()
                        }
                    },
                    onToggleAutoMode = { viewModel.setAutoMode(!state.autoMode) },
                    onEnd = { viewModel.endSessionEarly() }
                )

                "done" -> {
                    val habitsForLinking by viewModel.habitsForLinking.collectAsStateWithLifecycle()
                    ChantDone(
                        chant = state.selectedChant!!,
                        actualCount = state.currentCount,
                        targetCount = state.targetCount,
                        linkedHabitName = state.linkedHabitName,
                        habitsForLinking = habitsForLinking,
                        onLinkToHabit = { viewModel.linkSessionToHabit(it) },
                        onDone = {
                            viewModel.resetToPickerAfterDone()
                            onSessionComplete()
                        },
                        onSitAgain = {
                            viewModel.resetToPickerAfterDone()
                            state.selectedChant?.let { viewModel.selectChant(it) }
                        }
                    )
                }

                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChantPicker(
    chants: List<ChantDefinition>,
    onSelect: (ChantDefinition) -> Unit,
    onEdit: (ChantDefinition) -> Unit,
    onDelete: (ChantDefinition) -> Unit
) {
    var confirmDeleteChant by remember { mutableStateOf<ChantDefinition?>(null) }

    if (chants.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading chants…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    confirmDeleteChant?.let { chantToDelete ->
        AlertDialog(
            onDismissRequest = { confirmDeleteChant = null },
            title = { Text("Delete Chant") },
            text = { Text("Delete \"${chantToDelete.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(chantToDelete); confirmDeleteChant = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteChant = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(chants) { chant ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(chant) }
                            .padding(16.dp)
                    ) {
                        Text(chant.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (chant.tradition != null) {
                            Text(
                                text = chant.tradition,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${chant.defaultCount} counts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (chant.audioUri != null) {
                                Text(
                                    text = "· audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    if (!chant.isBuiltIn) {
                        IconButton(onClick = { onEdit(chant) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TextButton(
                        onClick = { confirmDeleteChant = chant },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChantConfirm(
    chant: ChantDefinition,
    targetCount: Int,
    autoMode: Boolean,
    autoIntervalSeconds: Int,
    autoSoundId: String,
    onAutoModeChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onSoundChange: (String) -> Unit,
    onBegin: () -> Unit,
    onBack: () -> Unit
) {
    val intervalOptions = listOf(1 to "1s", 2 to "2s", 3 to "3s", 5 to "5s", 8 to "8s", 10 to "10s")
    val soundOptions = listOf(
        "none"        to "Silent",
        "short_beep"  to "Beep",
        "double_beep" to "Double",
        "positive"    to "Ding",
        "alert"       to "Bell",
        "long_beep"   to "Gong"
    )
    val isAudioMode = chant.audioUri != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(chant.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (chant.tradition != null) {
            Text(chant.tradition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = chant.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }
        Text(
            text = "$targetCount counts · one mala",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isAudioMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Audio mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Each playback of the selected audio counts as one repetition. The session advances automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Sound per count", style = MaterialTheme.typography.titleSmall)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        soundOptions.forEach { (id, label) ->
                            FilterChip(
                                selected = autoSoundId == id,
                                onClick = { onSoundChange(id) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-advance", style = MaterialTheme.typography.titleSmall)
                        Switch(checked = autoMode, onCheckedChange = onAutoModeChange)
                    }
                    if (autoMode) {
                        Text("Interval", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            intervalOptions.forEach { (sec, label) ->
                                FilterChip(
                                    selected = autoIntervalSeconds == sec,
                                    onClick = { onIntervalChange(sec) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(onClick = onBegin, modifier = Modifier.fillMaxWidth()) {
            Text(if (isAudioMode) "Begin Audio Practice" else if (autoMode) "Begin Auto Practice" else "Begin Practice")
        }
        TextButton(onClick = onBack) {
            Text("← Choose another")
        }
    }
}

@Composable
private fun ChantSession(
    chant: ChantDefinition,
    currentCount: Int,
    targetCount: Int,
    autoMode: Boolean,
    isAudioMode: Boolean,
    audioUnavailable: Boolean = false,
    onTap: () -> Unit,
    onToggleAutoMode: () -> Unit,
    onEnd: () -> Unit
) {
    val progress = if (targetCount > 0) currentCount.toFloat() / targetCount.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            if (audioUnavailable) {
                Text(
                    "Audio file could not be loaded. Edit this chant to re-select.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(chant.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = chant.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 4
            )
        }

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(240.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            FilledTonalButton(
                onClick = onTap,
                enabled = !autoMode && !isAudioMode,
                modifier = Modifier.size(180.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$currentCount", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "of $targetCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAudioMode) {
                        Text("playing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    } else if (autoMode) {
                        Text("auto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isAudioMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Auto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = autoMode, onCheckedChange = { onToggleAutoMode() })
                }
            }
            OutlinedButton(
                onClick = onEnd,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("End Practice")
            }
        }
    }
}

@Composable
private fun ChantDone(
    chant: ChantDefinition,
    actualCount: Int,
    targetCount: Int,
    linkedHabitName: String?,
    habitsForLinking: List<HabitDefinition>,
    onLinkToHabit: (Long) -> Unit,
    onDone: () -> Unit,
    onSitAgain: () -> Unit
) {
    var showHabitPicker by remember { mutableStateOf(false) }

    if (showHabitPicker) {
        HabitLinkPickerDialog(
            habits = habitsForLinking,
            onDismiss = { showHabitPicker = false },
            onSelect = { habitId ->
                onLinkToHabit(habitId)
                showHabitPicker = false
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
        Text("📿", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        if (actualCount >= targetCount) {
            Text(
                text = "Practice complete.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$actualCount counts. One mala complete.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Session ended.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$actualCount of $targetCount counts · ${chant.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(32.dp))
        if (linkedHabitName != null) {
            Text(
                text = "Linked to “$linkedHabitName”",
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
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSitAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Sit again")
        }
    }
}

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
private fun ChantFormDialog(
    title: String,
    initial: ChantDefinition?,
    onDismiss: () -> Unit,
    onSave: (name: String, text: String, tradition: String, count: Int, audioUri: String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var tradition by remember { mutableStateOf(initial?.tradition ?: "") }
    var countText by remember { mutableStateOf((initial?.defaultCount ?: 108).toString()) }
    var pickedAudioUri by remember { mutableStateOf<String?>(initial?.audioUri) }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            pickedAudioUri = uri.toString()
        }
    }

    val audioFileName = pickedAudioUri?.let { uriStr ->
        runCatching {
            Uri.parse(uriStr).lastPathSegment?.substringAfterLast('/')
        }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Chant text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
                OutlinedTextField(
                    value = tradition,
                    onValueChange = { tradition = it },
                    label = { Text("Tradition (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Default count") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedButton(
                    onClick = { audioLauncher.launch("audio/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (audioFileName != null) "Audio: $audioFileName" else "Pick audio file (optional)")
                }
                if (pickedAudioUri != null) {
                    TextButton(
                        onClick = { pickedAudioUri = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove audio", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && text.isNotBlank()) {
                        onSave(name, text, tradition, countText.toIntOrNull() ?: 108, pickedAudioUri)
                    }
                },
                enabled = name.isNotBlank() && text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
