package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.LeafSectionItemCard
import com.example.habitpower.util.NotificationSoundOption
import com.example.habitpower.util.SoundPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationToneScreen(
    navigateBack: () -> Unit,
    viewModel: AdminNotificationToneViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Sound") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master sound toggle
            item {
                Spacer(Modifier.height(8.dp))
                ToneToggleCard(
                    title = "Completion Sound",
                    subtitle = if (state.soundEnabled) "Sound is on" else "Sound is off",
                    enabled = state.soundEnabled,
                    onToggle = { viewModel.toggleSound() }
                )
            }

            item {
                ToneToggleCard(
                    title = "Completion Vibration",
                    subtitle = if (state.vibrationEnabled) "Mild haptic on task completion" else "Vibration is off",
                    enabled = state.vibrationEnabled,
                    onToggle = { viewModel.toggleVibration() }
                )
            }

            item {
                Text(
                    text = "Choose Tone",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(state.availableOptions) { option ->
                val isSelected = option.id == state.selectedSoundId
                ToneOptionRow(
                    option = option,
                    isSelected = isSelected,
                    onSelect = { viewModel.selectSound(option) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                ToneToggleCard(
                    title = "Routine Exercise Start Sound",
                    subtitle = if (state.routineStartSoundEnabled) "Enabled for timed routines" else "Disabled",
                    enabled = state.routineStartSoundEnabled,
                    onToggle = { viewModel.toggleRoutineStartSound() }
                )
            }

            items(state.availableOptions) { option ->
                val isSelected = option.id == state.routineStartSoundId
                ToneOptionRow(
                    option = option,
                    isSelected = isSelected,
                    onSelect = { viewModel.selectRoutineStartSound(option) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                ToneToggleCard(
                    title = "Routine Exercise End Sound",
                    subtitle = if (state.routineEndSoundEnabled) "Enabled for timed routines" else "Disabled",
                    enabled = state.routineEndSoundEnabled,
                    onToggle = { viewModel.toggleRoutineEndSound() }
                )
            }

            items(state.availableOptions) { option ->
                val isSelected = option.id == state.routineEndSoundId
                ToneOptionRow(
                    option = option,
                    isSelected = isSelected,
                    onSelect = { viewModel.selectRoutineEndSound(option) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ToneToggleCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    LeafSectionItemCard(
        title = title,
        subtitle = subtitle,
        attributes = listOf("State" to if (enabled) "On" else "Off"),
        trailingActions = {
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() }
            )
        }
    )
}

@Composable
private fun ToneOptionRow(
    option: NotificationSoundOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    LeafSectionItemCard(
        title = option.displayName,
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        trailingActions = {
            RadioButton(selected = isSelected, onClick = onSelect)
            IconButton(onClick = { SoundPlayer.play(option) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Preview ${option.displayName}")
            }
        }
    )
}
