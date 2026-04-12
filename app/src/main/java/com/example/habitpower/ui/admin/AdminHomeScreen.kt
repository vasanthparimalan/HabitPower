package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.theme.AppSpacing
import com.example.habitpower.ui.theme.SectionHeader
import com.example.habitpower.ui.theme.StatusChip
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navigateBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
    val coroutineScope = rememberCoroutineScope()
    val streakBaseDays by repository.streakBaseDays.collectAsState(initial = 7)
    var showStreakDialog by remember { mutableStateOf(false) }
    var starterStackStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val setupActions = listOf(
            AdminActionItem(
                title = "Users",
                description = "Create family members who will be tracked in the app.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminUsers.route) }
            ),
            AdminActionItem(
                title = "Habits",
                description = "Create reusable habits once, then assign them to each user.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminHabits.route) }
            ),
            AdminActionItem(
                title = "Life Areas",
                description = "Organize habits into domains like Health, Learning, and Family.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminLifeAreas.route) }
            )
        )

        val operationsActions = listOf(
            AdminActionItem(
                title = "Assignments",
                description = "Pick which habits and life areas belong to each user.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminAssignments.route) }
            ),
            AdminActionItem(
                title = "Atomic Quotes",
                description = "Manage motivational quotes shown on Dashboard and Widget.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminQuotes.route) }
            )
        )

        val systemActions = listOf(
            AdminActionItem(
                title = "Notification Sound",
                description = "Choose the tone for Focus and Pomodoro completion alerts.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminNotificationTone.route) }
            ),
            AdminActionItem(
                title = "Streak Milestones",
                description = "Set the base number of consecutive days for widget streak milestone emojis. Currently: $streakBaseDays days.",
                buttonLabel = "Edit",
                onClick = { showStreakDialog = true }
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Admin",
                    subtitle = "Simple setup path: Users -> Habits -> Assignments."
                )
                StatusChip(
                    text = "Pick one next action",
                    modifier = Modifier.padding(top = AppSpacing.xs),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            item {
                QuickStartCard(
                    onOpenUsers = { onNavigate(Screen.AdminUsers.route) },
                    onOpenHabits = { onNavigate(Screen.AdminHabits.route) },
                    onOpenAssignments = { onNavigate(Screen.AdminAssignments.route) },
                    onOpenGuide = { onNavigate(Screen.Help.route) },
                    onApplyStarterStack = {
                        coroutineScope.launch {
                            val createdCount = repository.applyStarterHabitStackForUser()
                            starterStackStatus = if (createdCount > 0) {
                                "Starter stack applied. Added $createdCount new habits."
                            } else {
                                "Starter stack already available for this user."
                            }
                        }
                    },
                    statusMessage = starterStackStatus
                )
            }

            item {
                SectionHeader(
                    title = "Setup",
                    subtitle = "Create people, habits, and life-area structure."
                )
            }
            items(setupActions) { action ->
                AdminActionCard(
                    title = action.title,
                    description = action.description,
                    buttonLabel = action.buttonLabel,
                    containerColor = MaterialTheme.colorScheme.surface,
                    onClick = action.onClick
                )
            }

            item {
                SectionHeader(
                    title = "Daily Operations",
                    subtitle = "Keep assignments and motivation content updated."
                )
            }
            items(operationsActions) { action ->
                AdminActionCard(
                    title = action.title,
                    description = action.description,
                    buttonLabel = action.buttonLabel,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = action.onClick
                )
            }

            item {
                SectionHeader(
                    title = "System",
                    subtitle = "Tune app-level sounds and streak behavior."
                )
            }
            items(systemActions) { action ->
                AdminActionCard(
                    title = action.title,
                    description = action.description,
                    buttonLabel = action.buttonLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = action.onClick
                )
            }
        }

        if (showStreakDialog) {
            var inputText by remember { mutableStateOf(streakBaseDays.toString()) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showStreakDialog = false },
                title = { Text("Streak Milestone Base") },
                text = {
                    Column {
                        Text("Enter the number of days for the base streak length (e.g. 7). Your widgets progress in milestones of 1x, 2x, and 3x of this base.")
                        androidx.compose.material3.OutlinedTextField(
                            value = inputText,
                            onValueChange = { newValue -> inputText = newValue.filter { char -> char.isDigit() } },
                            label = { Text("Days") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val value = inputText.toIntOrNull()?.takeIf { it > 0 } ?: 7
                            coroutineScope.launch {
                                repository.saveStreakBaseDays(value)
                            }
                            showStreakDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showStreakDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private data class AdminActionItem(
    val title: String,
    val description: String,
    val buttonLabel: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickStartCard(
    onOpenUsers: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenGuide: () -> Unit,
    onApplyStarterStack: () -> Unit,
    statusMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            SectionHeader(
                title = "Quick Start",
                subtitle = "If this is your first time, follow this order."
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(text = "1. Users")
                StatusChip(text = "2. Habits")
                StatusChip(text = "3. Assign")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenUsers, modifier = Modifier.weight(1f)) { Text("Users") }
                Button(onClick = onOpenHabits, modifier = Modifier.weight(1f)) { Text("Habits") }
                Button(onClick = onOpenAssignments, modifier = Modifier.weight(1f)) { Text("Assign") }
            }

            TextButton(onClick = onOpenGuide) {
                Text("Open Guide & Help")
            }

            TextButton(onClick = onApplyStarterStack) {
                Text("Apply 80/20 Starter Stack")
            }

            statusMessage?.let {
                StatusChip(text = it)
            }
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    description: String,
    buttonLabel: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}
