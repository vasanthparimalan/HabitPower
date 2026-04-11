package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navigateBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
    val streakBaseDays by repository.streakBaseDays.collectAsState(initial = 7)
    var showStreakDialog by remember { mutableStateOf(false) }

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
        val actions = listOf(
            AdminActionItem(
                title = "Manage Users",
                description = "Create people who will use the app and appear in the widget and app switchers.",
                buttonLabel = "Open Users",
                onClick = { onNavigate(Screen.AdminUsers.route) }
            ),
            AdminActionItem(
                title = "Manage Habits",
                description = "Create configurable daily habits with types, units, and optional targets.",
                buttonLabel = "Open Habits",
                onClick = { onNavigate(Screen.AdminHabits.route) }
            ),
            AdminActionItem(
                title = "Manage Life Areas",
                description = "Add, edit, and delete life areas used to organize habits.",
                buttonLabel = "Open Life Areas",
                onClick = { onNavigate(Screen.AdminLifeAreas.route) }
            ),
            AdminActionItem(
                title = "Notification Sound",
                description = "Choose the tone played when a Focus timer or Pomodoro session completes.",
                buttonLabel = "Open Sound Settings",
                onClick = { onNavigate(Screen.AdminNotificationTone.route) }
            ),
            AdminActionItem(
                title = "Assign Habits",
                description = "Choose which habits belong to each user's daily check-in.",
                buttonLabel = "Open Assignments",
                onClick = { onNavigate(Screen.AdminAssignments.route) }
            ),
            AdminActionItem(
                title = "Manage Atomic Quotes",
                description = "Add or remove quotes that display on the Dashboard and Widget.",
                buttonLabel = "Open Quotes",
                onClick = { onNavigate(Screen.AdminQuotes.route) }
            ),
            AdminActionItem(
                title = "Configure Streaks",
                description = "Set the base number of consecutive days for widget streak milestone emojis. Currently: $streakBaseDays days.",
                buttonLabel = "Edit Target",
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
                Text(
                    "Configure users and the daily habits that shape their check-ins.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(actions) { action ->
                AdminActionCard(
                    title = action.title,
                    description = action.description,
                    buttonLabel = action.buttonLabel,
                    onClick = action.onClick
                )
            }
        }

        if (showStreakDialog) {
            var inputText by remember { mutableStateOf(streakBaseDays.toString()) }
            val coroutineScope = rememberCoroutineScope()

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
private fun AdminActionCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}
