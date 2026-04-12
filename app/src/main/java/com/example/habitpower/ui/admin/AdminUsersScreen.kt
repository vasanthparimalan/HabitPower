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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.SectionHeader
import com.example.habitpower.ui.theme.StatusChip

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    navigateBack: () -> Unit,
    viewModel: AdminUsersViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val createSuccessTick by viewModel.createSuccessTick.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createSuccessTick) {
        if (createSuccessTick > 0L) {
            snackbarHostState.showSnackbar("User added successfully")
        }
    }

    var editingUser by remember { mutableStateOf<com.example.habitpower.data.model.UserProfile?>(null) }
    var userToDelete by remember { mutableStateOf<com.example.habitpower.data.model.UserProfile?>(null) }

    editingUser?.let { user ->
        var editName by remember { mutableStateOf(user.name) }
        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("Edit User") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("User name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateUser(user, editName)
                    editingUser = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) { Text("Cancel") }
            }
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete ${user.name}? This might remove their tracked habit data.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUser(user)
                    userToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(
                            title = "Add User",
                            subtitle = "Create profiles for each person using the app."
                        )
                        OutlinedTextField(
                            value = viewModel.newUserName,
                            onValueChange = viewModel::updateNewUserName,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("User name") }
                        )
                        Button(onClick = viewModel::createUser) {
                            Text("Create User")
                        }
                    }
                }
            }

            if (users.isEmpty()) {
                item {
                    Text("No users created yet.")
                }
            } else {
                items(users, key = { it.id }) { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (activeUser?.id == user.id) "Currently active" else "Available for selection",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingUser = user }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { userToDelete = user }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                                if (activeUser?.id == user.id) {
                                    StatusChip(text = "Active", modifier = Modifier.padding(start = 8.dp))
                                } else {
                                    TextButton(onClick = { viewModel.setActiveUser(user.id) }) {
                                        Text("Use")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
