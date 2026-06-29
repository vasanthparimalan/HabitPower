package com.example.habitpower.ui.report

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.AppSpacing
import com.example.habitpower.ui.theme.LeafSectionItemCard
import com.example.habitpower.ui.theme.SectionHeader
import com.example.habitpower.ui.theme.StatusChip
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitInventoryScreen(
    navigateBack: () -> Unit,
    viewModel: HabitInventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var retireCandidate by remember { mutableStateOf<HabitDefinition?>(null) }
    var pauseCandidate by remember { mutableStateOf<HabitDefinition?>(null) }

    retireCandidate?.let { habit ->
        AlertDialog(
            onDismissRequest = { retireCandidate = null },
            title = { Text("Retiring is wisdom, not failure") },
            text = {
                Text(
                    "You ran an experiment with \"${habit.name}\". " +
                    "Habits that don't fit can be honoured and let go — they taught you something.\n\n" +
                    "You can always reactivate it later from Admin → Habits."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.retireHabit(habit)
                    retireCandidate = null
                }) { Text("Yes, retire it") }
            },
            dismissButton = {
                TextButton(onClick = { retireCandidate = null }) { Text("Keep it for now") }
            }
        )
    }

    pauseCandidate?.let { habit ->
        AlertDialog(
            onDismissRequest = { pauseCandidate = null },
            title = { Text("Pause \"${habit.name}\"?") },
            text = { Text("Pausing keeps the habit in your library but removes it from daily check-in. Resume it any time from Admin → Habits.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.pauseHabit(habit)
                    pauseCandidate = null
                }) { Text("Pause it") }
            },
            dismissButton = {
                TextButton(onClick = { pauseCandidate = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Inventory") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "Habit Inventory",
                    subtitle = "30-day health check for each of your ${uiState.totalActive} active habits."
                )
            }

            // Overall summary card
            item {
                OverallSummaryCard(
                    totalActive = uiState.totalActive,
                    overallRatio = uiState.overallRatio,
                    _overCommitted = uiState.overCommitted
                )
            }

            // Over-commitment banner
            if (uiState.overCommitted) {
                item {
                    OverCommitmentCard()
                }
            }

            // Habits grouped by health state
            for (healthState in HabitHealthState.entries) {
                val habitsInState = uiState.habitsByHealth[healthState] ?: continue
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = healthState.label,
                        subtitle = healthState.description
                    )
                }
                items(habitsInState) { hwh ->
                    HabitHealthCard(
                        hwh = hwh,
                        healthState = healthState,
                        onRetire = { retireCandidate = hwh.habit },
                        onPause = { pauseCandidate = hwh.habit }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OverallSummaryCard(
    totalActive: Int,
    overallRatio: Float,
    _overCommitted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Overall — last 30 days",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${(overallRatio * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            LinearProgressIndicator(
                progress = { overallRatio.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            Text(
                "$totalActive active habits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun OverCommitmentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Spread too thin?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                "You have 7+ habits but under 50% overall completion. Protecting 3 non-negotiables " +
                "and pausing the rest often works better than running everything at 40%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun HabitHealthCard(
    hwh: HabitWithHealth,
    healthState: HabitHealthState,
    onRetire: () -> Unit,
    onPause: () -> Unit
) {
    val containerColor = when (healthState) {
        HabitHealthState.THRIVING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        HabitHealthState.STEADY -> MaterialTheme.colorScheme.secondaryContainer
        HabitHealthState.STRUGGLING -> MaterialTheme.colorScheme.tertiaryContainer
        HabitHealthState.STALE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }
    val completionText = if (hwh.scheduledDays > 0) {
        "${hwh.completedDays}/${hwh.scheduledDays} days  •  ${(hwh.completionRatio * 100).roundToInt()}%"
    } else {
        "No scheduled days in range"
    }

    LeafSectionItemCard(
        title = hwh.habit.name,
        subtitle = if (hwh.habit.goalIdentityStatement.isNotBlank()) hwh.habit.goalIdentityStatement
                   else hwh.habit.description,
        attributes = listOf("30-day completion" to completionText),
        containerColor = containerColor,
        trailingActions = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                if (healthState == HabitHealthState.STALE || healthState == HabitHealthState.STRUGGLING) {
                    TextButton(onClick = onPause) { Text("Pause") }
                    TextButton(onClick = onRetire) { Text("Retire") }
                }
            }
        }
    )
}
