package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = viewModel.newName,
                onValueChange = { viewModel.updateNewName(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            OutlinedTextField(
                value = viewModel.newDescription,
                onValueChange = { viewModel.updateNewDescription(it) },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Button(onClick = { viewModel.createLifeArea() }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Add Life Area")
            }

            LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
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
        AlertDialog(
            onDismissRequest = { editing.value = null },
            title = { Text("Edit Life Area") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName.value, onValueChange = { editName.value = it }, label = { Text("Name") })
                    OutlinedTextField(value = editDesc.value, onValueChange = { editDesc.value = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateLifeArea(area, editName.value, editDesc.value.ifBlank { null })
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name)
            item.description?.let {
                Text(text = it)
            }
        }
        Row {
            IconButton(onClick = { onEdit(item) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
