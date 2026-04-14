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
import com.example.habitpower.ui.theme.LeafSectionItemCard
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
            )
        )

        val dataActions = listOf(
            AdminActionItem(
                title = "Export Data",
                description = "Save your habit history as CSV for analysis or JSON as a full backup. You choose the file name and location.",
                buttonLabel = "Open",
                onClick = { onNavigate(Screen.AdminExport.route) }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "80/20 Starter Stack",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Instantly populate habits based on high-impact defaults. Safe to run — skips habits that already exist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    val createdCount = repository.applyStarterHabitStackForUser()
                                    starterStackStatus = if (createdCount > 0) {
                                        "Added $createdCount new habits."
                                    } else {
                                        "Already up to date — no new habits added."
                                    }
                                }
                            }) {
                                Text("Apply Starter Stack")
                            }
                            val status = starterStackStatus
                            if (status != null) {
                                StatusChip(text = status)
                            }
                        }
                    }
                }
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
                    subtitle = "Tune app-level notification sounds."
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

            item {
                SectionHeader(
                    title = "Data",
                    subtitle = "Export your history for analysis or backup."
                )
            }
            items(dataActions) { action ->
                AdminActionCard(
                    title = action.title,
                    description = action.description,
                    buttonLabel = action.buttonLabel,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = action.onClick
                )
            }
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
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    LeafSectionItemCard(
        title = title,
        subtitle = description,
        containerColor = containerColor,
        onClick = onClick,
        trailingActions = {
            TextButton(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    )
}
