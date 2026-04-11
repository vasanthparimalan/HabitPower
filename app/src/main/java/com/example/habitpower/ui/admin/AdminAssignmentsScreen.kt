package com.example.habitpower.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminAssignmentsScreen(
    navigateBack: () -> Unit,
    viewModel: AdminAssignmentsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val users by viewModel.users.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val selectedUserId by viewModel.selectedUserId.collectAsState()
    val selectedHabitIds by viewModel.selectedHabitIds.collectAsState()
    val saveSuccessTick by viewModel.saveSuccessTick.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveSuccessTick) {
        if (saveSuccessTick > 0L) {
            snackbarHostState.showSnackbar("Habit assignments saved")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Assign Habits") },
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
                        Text("Select User", style = MaterialTheme.typography.titleMedium)
                        UserSelectionMenu(
                            users = users,
                            selectedUserId = selectedUserId,
                            onSelectUser = viewModel::setSelectedUser
                        )
                        Button(
                            onClick = viewModel::saveAssignments,
                            enabled = selectedUserId != null
                        ) {
                            Text("Save Assignments")
                        }
                    }
                }
            }

            if (selectedUserId == null) {
                item {
                    Text("Create a user first to assign habits.")
                }
            } else if (habits.isEmpty()) {
                item {
                    Text("Create habits first before assigning them.")
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitAssignmentCard(
                        habit = habit,
                        checked = selectedHabitIds.contains(habit.id),
                        onCheckedChange = { viewModel.toggleHabit(habit.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSelectionMenu(
    users: List<UserProfile>,
    selectedUserId: Long?,
    onSelectUser: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(users.firstOrNull { it.id == selectedUserId }?.name ?: "Choose user")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select user")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            users.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.name) },
                    onClick = {
                        expanded = false
                        onSelectUser(user.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun HabitAssignmentCard(
    habit: HabitDefinition,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildString {
                        append(habit.type.name)
                        habit.unit?.takeIf { it.isNotBlank() }?.let {
                            append(" • ")
                            append(it)
                        }
                        habit.targetValue?.let {
                            append(" • target ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
