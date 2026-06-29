package com.example.habitpower.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.SectionHeader

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    navigateBack: () -> Unit,
    viewModel: ImportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state = viewModel.state

    val hpexLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromHpex(it) }
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromJson(it) }
    }
    val habitsCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromHabitsCsv(it) }
    }
    val routinesCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromRoutinesCsv(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore from Backup") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                is ImportState.Idle -> IdleContent(
                    onSelectHpex = { hpexLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                    onSelectJson = { jsonLauncher.launch(arrayOf("application/json", "*/*")) },
                    onSelectHabitsCsv = { habitsCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                    onSelectRoutinesCsv = { routinesCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }
                )
                is ImportState.Importing -> ImportingContent()
                is ImportState.Success -> SuccessContent(result = state.result, onDone = navigateBack)
                is ImportState.Error -> ErrorContent(message = state.message, onRetry = { viewModel.reset() })
            }
        }
    }
}

@Composable
private fun IdleContent(
    onSelectHpex: () -> Unit,
    onSelectJson: () -> Unit,
    onSelectHabitsCsv: () -> Unit,
    onSelectRoutinesCsv: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Restore from Backup",
                subtitle = "Select your backup file to restore data. A .hpex file restores everything exactly as it was."
            )
        }

        // ── Primary: .hpex ─────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "HabitPower Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            ".hpex",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Restores your complete app state — users, life areas, habits, tracking history, streaks, routines, chants, tasks, and custom quotes. Everything comes back with original IDs and settings intact.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    listOf(
                        "Users, profiles, and life area assignments",
                        "Habits with full recurrence schedules",
                        "Complete tracking history",
                        "Streaks, XP, badges, and practice depth",
                        "Routines, exercises, chants, tasks, quotes"
                    ).forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Button(onClick = onSelectHpex, modifier = Modifier.align(Alignment.End)) {
                        Text("Select .hpex File")
                    }
                }
            }
        }

        // ── Legacy / other formats ──────────────────────────────────────────
        item {
            SectionHeader(
                title = "Legacy Formats",
                subtitle = "Use these if you are migrating from the old app (com.example.healthtrack) or only have CSV/JSON exports."
            )
        }

        item {
            ImportOptionCard(
                title = "Full Backup",
                format = "JSON",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                includes = listOf(
                    "Users and habit definitions",
                    "Complete tracking history",
                    "Streaks, XP, and level",
                    "Health stats (sleep, steps)"
                ),
                limitations = listOf(
                    "Recurrence schedules default to Daily — re-configure after import",
                    "Life areas not included in this format"
                ),
                buttonLabel = "Select JSON Backup",
                onSelect = onSelectJson
            )
        }

        item {
            ImportOptionCard(
                title = "Habit History",
                format = "CSV",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onContainerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                includes = listOf(
                    "Users (created from names in the file)",
                    "Habits (name and type only)",
                    "Full tracking history (all dated entries)"
                ),
                limitations = listOf(
                    "Recurrence schedules default to Daily",
                    "No targets, life areas, or gamification stats"
                ),
                buttonLabel = "Select Habits CSV",
                onSelect = onSelectHabitsCsv
            )
        }

        item {
            ImportOptionCard(
                title = "Routines",
                format = "CSV",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onContainerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                includes = listOf(
                    "Routine names and structure",
                    "Exercises with sets, reps, and duration"
                ),
                limitations = listOf(
                    "Exercise images will be missing — re-link images in each exercise if needed"
                ),
                buttonLabel = "Select Routines CSV",
                onSelect = onSelectRoutinesCsv
            )
        }
    }
}

@Composable
private fun ImportOptionCard(
    title: String,
    format: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onContainerColor: androidx.compose.ui.graphics.Color,
    includes: List<String>,
    limitations: List<String>,
    buttonLabel: String,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = onContainerColor, fontWeight = FontWeight.SemiBold)
                Text(format, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            includes.forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = onContainerColor)
            }
            if (limitations.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Limitations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                limitations.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall, color = onContainerColor)
                }
            }
            Button(onClick = onSelect, modifier = Modifier.align(Alignment.End)) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun ImportingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
            Text("Restoring your data…", style = MaterialTheme.typography.bodyLarge)
            Text("This may take a moment for large histories.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SuccessContent(result: ImportResult, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Restore complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mapOf(
                        "Users" to result.users,
                        "Life areas" to result.lifeAreas,
                        "Habits" to result.habits,
                        "Tracking entries" to result.entries,
                        "Health stats" to result.healthStats,
                        "Chants" to result.chants,
                        "Routines" to result.routines,
                        "Tasks / checklists" to result.tasks,
                        "Quotes" to result.quotes
                    ).filter { it.value > 0 }.forEach { (label, count) ->
                        SummaryRow(label, count)
                    }
                }
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Restore failed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
        }
    }
}

@Composable
private fun SummaryRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
