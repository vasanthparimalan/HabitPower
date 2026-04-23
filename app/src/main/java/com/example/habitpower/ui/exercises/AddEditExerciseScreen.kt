package com.example.habitpower.ui.exercises

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.ExerciseLibraryItem
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExerciseScreen(
    navigateBack: () -> Unit,
    viewModel: AddEditExerciseViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.processImage(context, uri)
    }

    var showLibraryDialog by remember { mutableStateOf(false) }

    if (showLibraryDialog) {
        ExerciseLibraryDialog(
            libraryItems = viewModel.libraryRepository.getAll(),
            onSelect = { item ->
                viewModel.prefillFromLibrary(item)
                showLibraryDialog = false
            },
            onDismiss = { showLibraryDialog = false }
        )
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
                    ExerciseImage(
                        imageUri = viewModel.imageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        exerciseName = viewModel.name.ifBlank { "Tap to select image" },
                        category = viewModel.category,
                        detailLabel = viewModel.description.ifBlank {
                            "Bundled and picked images are shown here."
                        },
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = "Best format: .webp or .png (1080p+ recommended). Use 16:9 or 4:3 aspect ratio for the best view during workouts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { showLibraryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Browse Exercise Library")
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

            // Category selection
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExerciseCategory.entries) { cat ->
                        FilterChip(
                            selected = viewModel.category == cat,
                            onClick = { viewModel.updateCategory(cat) },
                            label = { Text(cat.displayName) },
                            leadingIcon = {
                                if (viewModel.category == cat) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }

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
                        if (!viewModel.isTimeBased) Icon(Icons.Default.Check, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = viewModel.isTimeBased,
                    onClick = { viewModel.setTimeBasedMode(true) },
                    label = { Text("Time Based") },
                    leadingIcon = {
                        if (viewModel.isTimeBased) Icon(Icons.Default.Check, contentDescription = null)
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

@Composable
private fun ExerciseLibraryDialog(
    libraryItems: List<ExerciseLibraryItem>,
    onSelect: (ExerciseLibraryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedSections = remember {
        mutableStateMapOf<ExerciseCategory, Boolean>().apply {
            ExerciseCategory.entries.forEach { category -> this[category] = true }
        }
    }
    fun isExpanded(category: ExerciseCategory) = expandedSections[category] == true

    val filtered = libraryItems.filter { item ->
        searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
    }
    val grouped = ExerciseCategory.entries.associateWith { category ->
        filtered.filter { it.category == category }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exercise Library") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    ExerciseCategory.entries.forEach { category ->
                        val itemsInCategory = grouped.getValue(category)
                        item(key = "${category.name}_header") {
                            CategorySectionHeader(
                                title = category.displayName,
                                count = itemsInCategory.size,
                                isExpanded = isExpanded(category),
                                onToggle = { expandedSections[category] = !isExpanded(category) }
                            )
                        }
                        if (isExpanded(category)) {
                            if (itemsInCategory.isEmpty()) {
                                item(key = "${category.name}_empty") {
                                    Text(
                                        text = if (searchQuery.isBlank()) {
                                            "No bundled ${category.displayName.lowercase()} exercises yet."
                                        } else {
                                            "No matches in ${category.displayName.lowercase()}."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            } else {
                                items(itemsInCategory, key = { "${category.name}_${it.name}" }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelect(item) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExerciseImage(
                                            imageUri = item.imageUri,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .width(56.dp)
                                                .height(56.dp),
                                            exerciseName = item.name,
                                            category = item.category,
                                            detailLabel = item.primaryMuscle,
                                            contentScale = ContentScale.Crop,
                                            iconSize = 24.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                            item.primaryMuscle?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            item.instructions
                                                ?.lineSequence()
                                                ?.firstOrNull { line -> line.isNotBlank() }
                                                ?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
