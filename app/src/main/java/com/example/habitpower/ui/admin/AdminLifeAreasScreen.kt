package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.LeafSectionItemCard
import com.example.habitpower.ui.theme.SectionHeader

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminLifeAreasScreen(
    navigateBack: () -> Unit,
    viewModel: AdminLifeAreasViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val lifeAreas by viewModel.lifeAreas.collectAsState()
    val createSuccessTick by viewModel.createSuccessTick.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createSuccessTick) {
        if (createSuccessTick > 0L) {
            snackbarHostState.showSnackbar("Life area added successfully")
        }
    }

    // Editing state needs to be in the composable scope (not nested inside Column)
    val editing = remember { mutableStateOf<LifeArea?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Life Areas") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "Add Life Area",
                            subtitle = "Create categories to organize habits with cleaner reporting."
                        )
                        OutlinedTextField(
                            value = viewModel.newName,
                            onValueChange = { viewModel.updateNewName(it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = viewModel.newDescription,
                            onValueChange = { viewModel.updateNewDescription(it) },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = viewModel.newEmoji,
                            onValueChange = { viewModel.updateNewEmoji(it) },
                            label = { Text("Emoji (optional)") },
                            placeholder = { Text("e.g. 🏃") },
                            modifier = Modifier.fillMaxWidth(0.4f),
                            singleLine = true
                        )

                        Button(onClick = { viewModel.createLifeArea() }) {
                            Text("Add Life Area")
                        }
                    }
                }
            }

            if (lifeAreas.isEmpty()) {
                item {
                    Text("No life areas yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(lifeAreas) { item ->
                    LifeAreaRow(item, onEdit = { editing.value = it }, onDelete = { viewModel.deleteLifeArea(it) })
                }
            }
        }
    }

    // Simple edit dialog handling
    editing.value?.let { area ->
        val editName = remember(area.id) { mutableStateOf(area.name) }
        val editDesc = remember(area.id) { mutableStateOf(area.description ?: "") }
        val editEmoji = remember(area.id) { mutableStateOf(area.emoji ?: "") }
        AlertDialog(
            onDismissRequest = { editing.value = null },
            title = { Text("Edit Life Area") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName.value, onValueChange = { editName.value = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDesc.value, onValueChange = { editDesc.value = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = editEmoji.value,
                        onValueChange = { editEmoji.value = it.takeLast(2) },
                        label = { Text("Emoji") },
                        placeholder = { Text("e.g. 🏃") },
                        modifier = Modifier.fillMaxWidth(0.4f),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateLifeArea(area, editName.value, editDesc.value.ifBlank { null }, editEmoji.value.ifBlank { null })
                    editing.value = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editing.value = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LifeAreaRow(item: LifeArea, onEdit: (LifeArea) -> Unit, onDelete: (LifeArea) -> Unit) {
    LeafSectionItemCard(
        title = if (item.emoji != null) "${item.emoji} ${item.name}" else item.name,
        subtitle = item.description,
        trailingActions = {
            IconButton(onClick = { onEdit(item) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}
