package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.theme.LeafSectionItemCard
import com.example.habitpower.ui.theme.StatusChip
import com.example.habitpower.util.LocalCrashHandler
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
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetFinalConfirm by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }

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
        val allActions = listOf(
            AdminActionItem("Users", "Add profiles for each person.") { onNavigate(Screen.AdminUsers.route) },
            AdminActionItem("Habits", "Define habits, then assign to each person.") { onNavigate(Screen.AdminHabits.createRoute()) },
            AdminActionItem("Life Areas", "Group habits by life domain.") { onNavigate(Screen.AdminLifeAreas.route) },
            AdminActionItem("Assignments", "Assign habits and areas to each person.") { onNavigate(Screen.AdminAssignments.route) },
            AdminActionItem("Notification Channels", "Active notification types (max 2).") { onNavigate(Screen.AdminNotificationChannels.route) },
            AdminActionItem("Sound Settings", "Completion sounds for habits and sessions.") { onNavigate(Screen.AdminNotificationTone.route) },
            AdminActionItem("Atomic Quotes", "Edit motivational quotes.") { onNavigate(Screen.AdminQuotes.route) },
            AdminActionItem("Export Data", "Export habit history as CSV or JSON.") { onNavigate(Screen.AdminExport.route) },
            AdminActionItem("Restore from Backup", "Import data from a JSON backup file.") { onNavigate(Screen.AdminImport.route) },
            AdminActionItem("Google Drive Sync", "Auto-backup to Google Drive.") { onNavigate(Screen.AdminDriveSync.route) }
        )

        val crashLog = LocalCrashHandler.lastCrashLog
        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        var crashCopied by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (crashLog != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Crash Detected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "A crash was recorded on the previous session. Share this log with the developer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                            Text(
                                text = crashLog,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(crashLog))
                                    crashCopied = true
                                }) {
                                    Text(
                                        if (crashCopied) "Copied!" else "Copy to clipboard",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButton(onClick = {
                                    LocalCrashHandler.clearCrashLog(context)
                                }) {
                                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }

            items(allActions) { action ->
                AdminActionCard(
                    title = action.title,
                    description = action.description,
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
                        Text("80/20 Starter Stack", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Apply 8 high-impact default habits. Skips existing ones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
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
                            }) { Text("Apply Starter Stack") }
                            val status = starterStackStatus
                            if (status != null) { StatusChip(text = status) }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Reset App to Fresh State",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Deletes all users, habits, history, routines, tasks, and stats. Use this to fix a broken state before restoring a .hpex backup. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isResetting) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else if (resetDone) {
                                StatusChip(text = "Reset complete. Restore a backup now.")
                            } else {
                                OutlinedButton(
                                    onClick = { showResetConfirm = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Reset Everything")
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    // ── Step 1 confirmation ───────────────────────────────────────────────────
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset app data?") },
            text = {
                Text("This will permanently delete all users, habits, tracking history, routines, tasks, streaks, and stats. The app will be empty — ready for a fresh start or a .hpex restore.\n\nThis cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { showResetConfirm = false; showResetFinalConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── Step 2 final confirmation ─────────────────────────────────────────────
    if (showResetFinalConfirm) {
        AlertDialog(
            onDismissRequest = { showResetFinalConfirm = false },
            title = { Text("Are you absolutely sure?") },
            text = { Text("There is no undo. Every piece of data will be erased permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetFinalConfirm = false
                        isResetting = true
                        coroutineScope.launch {
                            repository.resetAllData()
                            isResetting = false
                            resetDone = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetFinalConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private data class AdminActionItem(
    val title: String,
    val description: String,
    val onClick: () -> Unit
)

@Composable
private fun AdminActionCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    LeafSectionItemCard(
        title = title,
        subtitle = description,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        trailingActions = {
            TextButton(onClick = onClick) { Text("Open") }
        }
    )
}
