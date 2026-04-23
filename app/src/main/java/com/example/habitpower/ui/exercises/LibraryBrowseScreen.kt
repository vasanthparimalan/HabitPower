package com.example.habitpower.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.ExerciseLibraryItem
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.LeafSectionItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBrowseScreen(
    navigateBack: () -> Unit,
    viewModel: LibraryBrowseViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val items by viewModel.filteredItems.collectAsState()
    val addedNames by viewModel.addedNames.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search exercises…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCat == null,
                        onClick = { viewModel.selectedCategory.value = null },
                        label = { Text("All") }
                    )
                }
                items(ExerciseCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCat == cat,
                        onClick = {
                            viewModel.selectedCategory.value = if (selectedCat == cat) null else cat
                        },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            Text(
                text = "${items.size} exercises",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(items, key = { it.name }) { item ->
                    val isAdded = addedNames.contains(item.name.trim().lowercase())
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LibraryExerciseItem(
                            item = item,
                            isAdded = isAdded,
                            onToggle = {
                                if (isAdded) viewModel.removeFromMyExercises(item.name)
                                else viewModel.addToMyExercises(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryExerciseItem(
    item: ExerciseLibraryItem,
    isAdded: Boolean,
    onToggle: () -> Unit
) {
    LeafSectionItemCard(
        title = item.name,
        subtitle = listOfNotNull(item.primaryMuscle, item.category.displayName).joinToString(" · "),
        attributes = emptyList(),
        leading = {
            ExerciseImage(
                imageUri = item.imageUri,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                exerciseName = item.name,
                category = item.category,
                iconSize = 26.dp
            )
        },
        trailingActions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdded) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Added",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(22.dp)
                    )
                } else {
                    IconButton(onClick = onToggle) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add to my exercises",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}
