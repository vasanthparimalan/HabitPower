package com.example.habitpower.ui.exercises

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExerciseScreen(
    navigateBack: () -> Unit,
    viewModel: AddEditExerciseViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.processImage(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.saveExercise()
                    navigateBack()
                }
            ) {
                Text("Save")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Picker Area
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    if (viewModel.imageUri != null) {
                        AsyncImage(
                            model = viewModel.imageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Text("Tap to select image")
                            }
                        }
                    }
                }
                Text(
                    text = "Best format: .webp or .png (1080p+ recommended). Use 16:9 or 4:3 aspect ratio for the best view during workouts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.tags,
                onValueChange = viewModel::updateTags,
                label = { Text("Tags (CSV, optional)") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("e.g. gym, bodyweight, yoga, flexibility, stretches, cooldown") }
            )

            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Important Notes (e.g. form cues)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = viewModel.instructions,
                onValueChange = viewModel::updateInstructions,
                label = { Text("Instructions (Step-by-step, optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                supportingText = { Text("e.g. 1. Warm up for 2 min\n2. Do 3 sets\n3. Rest between sets") }
            )

            OutlinedTextField(
                value = viewModel.targetSets,
                onValueChange = viewModel::updateTargetSets,
                label = { Text("Target Sets (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Reps vs Duration Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterChip(
                    selected = !viewModel.isTimeBased,
                    onClick = { viewModel.setTimeBasedMode(false) },
                    label = { Text("Reps Based") },
                    leadingIcon = {
                        if (!viewModel.isTimeBased) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )

                FilterChip(
                    selected = viewModel.isTimeBased,
                    onClick = { viewModel.setTimeBasedMode(true) },
                    label = { Text("Time Based") },
                    leadingIcon = {
                        if (viewModel.isTimeBased) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }

            if (!viewModel.isTimeBased) {
                OutlinedTextField(
                    value = viewModel.targetReps,
                    onValueChange = viewModel::updateTargetReps,
                    label = { Text("Target Reps") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else {
                OutlinedTextField(
                    value = viewModel.targetDuration,
                    onValueChange = viewModel::updateTargetDuration,
                    label = { Text("Target Duration (Seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}
