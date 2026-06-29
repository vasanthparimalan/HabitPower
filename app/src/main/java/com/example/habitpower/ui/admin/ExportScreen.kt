package com.example.habitpower.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.HpexSection
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navigateBack: () -> Unit,
    viewModel: ExportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    val hpexLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.writeTo(it) } }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.writeTo(it) } }

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.writeTo(it) } }

    LaunchedEffect(uiState.pendingExport) {
        val pending = uiState.pendingExport ?: return@LaunchedEffect
        when (pending.mimeType) {
            "application/octet-stream" -> hpexLauncher.launch(pending.suggestedFileName)
            "text/csv" -> csvLauncher.launch(pending.suggestedFileName)
            "application/json" -> jsonLauncher.launch(pending.suggestedFileName)
        }
    }

    LaunchedEffect(uiState.resultMessage) {
        val msg = uiState.resultMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearResult()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Export Your Data",
                    subtitle = "Your data stays on your device. You choose exactly where each file is saved."
                )
            }

            // ── .hpex backup ──────────────────────────────────────────────────
            item {
                HpexExportCard(
                    selectedSections = uiState.selectedHpexSections,
                    onToggleSection = { viewModel.toggleHpexSection(it) },
                    isPreparing = uiState.isPreparing,
                    onExport = { viewModel.prepareHpexExport() }
                )
            }

            // ── Spreadsheet / Analysis exports ─────────────────────────────────
            item {
                SectionHeader(
                    title = "Spreadsheet Exports",
                    subtitle = "Open in Excel or Google Sheets for analysis."
                )
            }

            item {
                ExportFormatCard(
                    title = "Habit History",
                    format = "CSV",
                    description = "One row per habit entry — ready to open in Excel or Google Sheets.",
                    includes = listOf(
                        "Columns: date, user, habit_name, type, life_area, value, target, completed",
                        "ISO 8601 dates — sorts correctly in any spreadsheet",
                        "Sorted newest-first"
                    ),
                    buttonLabel = "Save Habits CSV",
                    isPreparing = uiState.isPreparing,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onExport = { viewModel.prepareExport(ExportFormat.CSV) }
                )
            }

            item {
                ExportFormatCard(
                    title = "Routines",
                    format = "CSV",
                    description = "All your routines and their exercises.",
                    includes = listOf(
                        "Columns: routine_name, exercise, sets, reps, duration_s, order",
                        "One row per exercise in each routine"
                    ),
                    buttonLabel = "Save Routines CSV",
                    isPreparing = uiState.isPreparing,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onExport = { viewModel.prepareExport(ExportFormat.ROUTINES_CSV) }
                )
            }

            item {
                ExportFormatCard(
                    title = "Health Stats",
                    format = "CSV",
                    description = "Daily health data. Columns: date, sleep_hours, steps.",
                    includes = listOf("Sorted newest-first"),
                    buttonLabel = "Save Health CSV",
                    isPreparing = uiState.isPreparing,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onExport = { viewModel.prepareExport(ExportFormat.HEALTH_CSV) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Opening in Excel or Google Sheets",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        listOf(
                            "1. Open Excel → File → Open → choose the CSV file",
                            "2. Freeze top row: View → Freeze Panes → Freeze Top Row",
                            "3. Filter by habit: Data → Filter → click dropdown on habit_name",
                            "4. Dates are ISO 8601 (YYYY-MM-DD) — sort correctly without reformatting"
                        ).forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("About your backup",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Your data is also automatically backed up by Android's Google Backup on your regular device backup schedule. The .hpex backup above is an additional manual option for full control.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.isPreparing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun HpexExportCard(
    selectedSections: Set<HpexSection>,
    onToggleSection: (HpexSection) -> Unit,
    isPreparing: Boolean,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HabitPower Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    ".hpex",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "A complete, portable snapshot of your app. Restore it on any device with a single tap — everything comes back exactly as it was.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "Choose what to include:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )

            HpexSection.entries.forEach { section ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = section in selectedSections,
                        onCheckedChange = { onToggleSection(section) }
                    )
                    Text(
                        section.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Button(
                onClick = onExport,
                enabled = !isPreparing && selectedSections.isNotEmpty(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Backup")
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    title: String,
    format: String,
    description: String,
    includes: List<String>,
    buttonLabel: String,
    isPreparing: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    format,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            includes.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onExport,
                enabled = !isPreparing,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
