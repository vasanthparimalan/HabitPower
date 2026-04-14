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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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

    // Two separate launchers — SAF requires MIME type at construction time
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.writeTo(it) } }

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.writeTo(it) } }

    // When ViewModel finishes preparing data, open the system file picker
    LaunchedEffect(uiState.pendingExport) {
        val pending = uiState.pendingExport ?: return@LaunchedEffect
        when (pending.mimeType) {
            "text/csv" -> csvLauncher.launch(pending.suggestedFileName)
            "application/json" -> jsonLauncher.launch(pending.suggestedFileName)
        }
    }

    // Show success/error message
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

            item {
                ExportFormatCard(
                    title = "Analysis Export",
                    format = "CSV",
                    description = "Opens in Excel, Google Sheets, or any spreadsheet app. One row per habit entry with date, user, habit name, type, and value.",
                    includes = listOf(
                        "All habit completion entries",
                        "User names and habit names included as columns",
                        "Compatible with any spreadsheet tool"
                    ),
                    buttonLabel = "Save CSV File",
                    isPreparing = uiState.isPreparing,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onExport = { viewModel.prepareExport(ExportFormat.CSV) }
                )
            }

            item {
                ExportFormatCard(
                    title = "Full Backup",
                    format = "JSON",
                    description = "A complete structured snapshot of everything in the app. Use this to safeguard your data or transfer it to a new device manually.",
                    includes = listOf(
                        "All users and habit definitions",
                        "Complete entry history",
                        "Health stats and gamification progress",
                        "Human-readable, structured format"
                    ),
                    buttonLabel = "Save JSON Backup",
                    isPreparing = uiState.isPreparing,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onExport = { viewModel.prepareExport(ExportFormat.JSON) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "About your backup",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Your data is also automatically backed up by Android's Google Backup on your regular device backup schedule. This export is an additional option for your own records or analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.isPreparing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            includes.forEach { item ->
                Text(
                    "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
