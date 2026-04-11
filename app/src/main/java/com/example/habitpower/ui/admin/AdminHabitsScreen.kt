package com.example.habitpower.ui.admin

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.reminder.HabitReminderScheduler
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminHabitsScreen(
    navigateBack: () -> Unit,
    viewModel: AdminHabitsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val habits by viewModel.habits.collectAsState()
    val context = LocalContext.current

    var editingHabit by remember { mutableStateOf<HabitDefinition?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitDefinition?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val canCreate = viewModel.name.trim().isNotBlank() &&
        viewModel.goalIdentityStatement.trim().isNotBlank() &&
        viewModel.description.trim().isNotBlank() &&
        viewModel.commitmentLocation.trim().isNotBlank() &&
        (!viewModel.preReminderEnabled || viewModel.preReminderMinutes.toIntOrNull() in 1..1440)

    val createSuccessTick by viewModel.createSuccessTick.collectAsState()
    LaunchedEffect(createSuccessTick) {
        if (createSuccessTick > 0L) {
            snackbarHostState.showSnackbar("Habit created successfully")
        }
    }

    editingHabit?.let { habit ->
        var editName by remember { mutableStateOf(habit.name) }
        var editDesc by remember { mutableStateOf(habit.description) }
        var editTarget by remember { mutableStateOf(habit.targetValue?.toString() ?: "") }
        var editOp by remember { mutableStateOf(habit.operator) }

        AlertDialog(
            onDismissRequest = { editingHabit = null },
            title = { Text("Edit Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (habit.type != HabitType.BOOLEAN && habit.type != HabitType.TEXT) {
                        OutlinedTextField(
                            value = editTarget,
                            onValueChange = { editTarget = it },
                            label = { Text("Target Value") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OperatorSelector(selected = editOp, onSelected = { editOp = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateHabit(habit, editName, editDesc, editTarget, editOp)
                    editingHabit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingHabit = null }) { Text("Cancel") }
            }
        )
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete '${habit.name}'? This will remove all tracked data for it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHabit(habit)
                    habitToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Habits") },
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
                        Text("Add Habit", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Fields marked * are required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedTextField(
                            value = viewModel.name,
                            onValueChange = viewModel::updateName,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Habit name *") }
                        )
                        OutlinedTextField(
                            value = viewModel.goalIdentityStatement,
                            onValueChange = viewModel::updateGoalIdentityStatement,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Type of person I want to be *") }
                        )
                        OutlinedTextField(
                            value = viewModel.description,
                            onValueChange = viewModel::updateDescription,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Goal description *") }
                        )
                        HabitTypeSelector(
                            selectedType = viewModel.selectedType,
                            onTypeSelected = viewModel::updateType
                        )

                        // Life area selector
                        val lifeAreas by viewModel.lifeAreas.collectAsState()
                        var lifeAreaExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { lifeAreaExpanded = true }) {
                                Text("Life area: ${lifeAreas.find { it.id == viewModel.selectedLifeAreaId }?.name ?: "None"}")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select life area")
                            }
                            DropdownMenu(expanded = lifeAreaExpanded, onDismissRequest = { lifeAreaExpanded = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { lifeAreaExpanded = false; viewModel.updateSelectedLifeArea(null) })
                                lifeAreas.forEach { la ->
                                    DropdownMenuItem(text = { Text(la.name) }, onClick = { lifeAreaExpanded = false; viewModel.updateSelectedLifeArea(la.id) })
                                }
                            }
                        }

                        Text("Commitment reminder", style = MaterialTheme.typography.titleSmall)
                        val commitmentTimeLabel = String.format(
                            java.util.Locale.US,
                            "Commitment time: %02d:%02d %s",
                            if (viewModel.commitmentHour % 12 == 0) 12 else viewModel.commitmentHour % 12,
                            viewModel.commitmentMinute,
                            if (viewModel.commitmentHour >= 12) "PM" else "AM"
                        )
                        Button(onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> viewModel.updateCommitmentTime(h, m) },
                                viewModel.commitmentHour,
                                viewModel.commitmentMinute,
                                false
                            ).show()
                        }) { Text(commitmentTimeLabel) }

                        OutlinedTextField(
                            value = viewModel.commitmentLocation,
                            onValueChange = viewModel::updateCommitmentLocation,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Commitment location *") }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Send reminder before commitment")
                            Switch(
                                checked = viewModel.preReminderEnabled,
                                onCheckedChange = viewModel::updatePreReminderEnabled
                            )
                        }

                        if (viewModel.preReminderEnabled) {
                            OutlinedTextField(
                                value = viewModel.preReminderMinutes,
                                onValueChange = viewModel::updatePreReminderMinutes,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Reminder minutes before (1-1440)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }

                        TextButton(onClick = { HabitReminderScheduler.openDndAccessSettings(context) }) {
                            Text("Allow reminders during DND (recommended)")
                        }

                        // TIME habit: show operator + time picker
                        if (viewModel.selectedType == HabitType.TIME) {
                            OperatorSelector(
                                selected = viewModel.selectedOperator,
                                onSelected = viewModel::updateOperator
                            )
                            val timeLabel = String.format(
                                java.util.Locale.US,
                                "Target: %02d:%02d %s",
                                if (viewModel.targetHour % 12 == 0) 12 else viewModel.targetHour % 12,
                                viewModel.targetMinute,
                                if (viewModel.targetHour >= 12) "PM" else "AM"
                            )
                            Button(onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> viewModel.updateTargetTime(h, m) },
                                    viewModel.targetHour,
                                    viewModel.targetMinute,
                                    false
                                ).show()
                            }) { Text(timeLabel) }
                        } else if (viewModel.selectedType == HabitType.TIMER) {
                            // TIMER: fixed unit "minutes", integer target 1–1440
                            Text(
                                text = "Unit: minutes (fixed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = viewModel.targetValue,
                                onValueChange = { v ->
                                    val n = v.filter { it.isDigit() }
                                    val clamped = n.toIntOrNull()?.coerceIn(1, 1440)?.toString() ?: n
                                    viewModel.updateTargetValue(clamped)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Duration (minutes, max 1440)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                supportingText = { Text("Max 1440 min = 24 hrs") }
                            )
                        } else if (viewModel.selectedType != HabitType.BOOLEAN && viewModel.selectedType != HabitType.TEXT) {
                            // Numeric habits: show unit + optional numeric target + operator
                            OutlinedTextField(
                                value = viewModel.unit,
                                onValueChange = viewModel::updateUnit,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Unit") }
                            )
                            OutlinedTextField(
                                value = viewModel.targetValue,
                                onValueChange = viewModel::updateTargetValue,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Target value (optional)") }
                            )
                            if (viewModel.targetValue.isNotBlank()) {
                                OperatorSelector(
                                    selected = viewModel.selectedOperator,
                                    onSelected = viewModel::updateOperator
                                )
                            }
                        }

                        Button(onClick = viewModel::createHabit, enabled = canCreate) {
                            Text("Create Habit")
                        }
                    }
                }
            }

            if (habits.isEmpty()) {
                item { Text("No habits created yet.") }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitSummaryCard(
                        habit = habit,
                        onEdit = { editingHabit = habit },
                        onDelete = { habitToDelete = habit }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitTypeSelector(
    selectedType: HabitType,
    onTypeSelected: (HabitType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Type: ${selectedType.name}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select habit type")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HabitType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        expanded = false
                        onTypeSelected(type)
                    }
                )
            }
        }
    }
}

@Composable
private fun OperatorSelector(
    selected: TargetOperator,
    onSelected: (TargetOperator) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        TargetOperator.LESS_THAN_OR_EQUAL to "≤  (Before / At most)",
        TargetOperator.GREATER_THAN_OR_EQUAL to "≥  (After / At least)",
        TargetOperator.EQUAL to "=  (Exactly)"
    )
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Condition: ${labels[selected]}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select operator")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TargetOperator.values().forEach { op ->
                DropdownMenuItem(
                    text = { Text(labels[op] ?: op.name) },
                    onClick = { expanded = false; onSelected(op) }
                )
            }
        }
    }
}

@Composable
private fun HabitSummaryCard(
    habit: HabitDefinition,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                if (habit.description.isNotBlank()) {
                    Text(habit.description, style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Type: ${habit.type.name}", style = MaterialTheme.typography.bodySmall)
                    habit.unit?.takeIf { it.isNotBlank() }?.let {
                        Text("Unit: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    habit.targetValue?.let {
                        val opLabel = when (habit.operator) {
                            TargetOperator.LESS_THAN_OR_EQUAL -> "≤"
                            TargetOperator.GREATER_THAN_OR_EQUAL -> "≥"
                            TargetOperator.EQUAL -> "="
                        }
                        if (habit.type == HabitType.TIME) {
                            val totalMins = ((it + 12 * 60) % (24 * 60)).toInt()
                            val h = totalMins / 60
                            val m = totalMins % 60
                            val amPm = if (h >= 12) "PM" else "AM"
                            val h12 = if (h % 12 == 0) 12 else h % 12
                            Text(
                                "Sleep $opLabel %02d:%02d $amPm".format(h12, m),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text("Target $opLabel $it ${habit.unit ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
