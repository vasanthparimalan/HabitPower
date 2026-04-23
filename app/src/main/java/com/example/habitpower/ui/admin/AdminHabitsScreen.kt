package com.example.habitpower.ui.admin

import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.reminder.HabitReminderScheduler
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.AppSpacing
import com.example.habitpower.ui.theme.LeafSectionItemCard
import com.example.habitpower.ui.theme.SectionHeader
import java.time.DayOfWeek

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminHabitsScreen(
    navigateBack: () -> Unit,
    viewModel: AdminHabitsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val habits by viewModel.habits.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val lifeAreas by viewModel.lifeAreas.collectAsState()
    val context = LocalContext.current

    var editingHabit by remember { mutableStateOf<HabitDefinition?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitDefinition?>(null) }
    var showHabitTypeInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val canCreate = viewModel.name.trim().isNotBlank() &&
        viewModel.goalIdentityStatement.trim().isNotBlank() &&
        viewModel.description.trim().isNotBlank() &&
        viewModel.commitmentLocation.trim().isNotBlank() &&
        (viewModel.selectedType != HabitType.ROUTINE || viewModel.selectedRoutineId != null) &&
        (!viewModel.preReminderEnabled || viewModel.preReminderMinutes.toIntOrNull() in 1..1440) &&
        viewModel.recurrenceStartDateText.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) &&
        (viewModel.recurrenceEndDateText.isBlank() || viewModel.recurrenceEndDateText.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) &&
        when (viewModel.selectedRecurrenceType) {
            HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> viewModel.selectedWeekdaysMask != 0
            HabitRecurrenceType.EVERY_N_DAYS -> viewModel.everyNDaysInterval.toIntOrNull()?.let { it >= 1 } == true
            HabitRecurrenceType.MONTHLY_BY_DATE -> viewModel.monthlyDayOfMonth.toIntOrNull() in 1..31
            HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                val w = viewModel.monthlyNthWeek.toIntOrNull()
                val d = viewModel.monthlyNthWeekday.toIntOrNull()
                (w in 1..5 || w == -1) && d in 1..7
            }
            HabitRecurrenceType.YEARLY_BY_DATE,
            HabitRecurrenceType.YEARLY_MULTI_DATE -> viewModel.yearlyDatesCsv.split(',').any {
                it.trim().matches(Regex("^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$"))
            }
            HabitRecurrenceType.DAILY -> true
        }

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
        var editRoutineId by remember { mutableStateOf(habit.routineId) }
        var editLifeAreaId by remember { mutableStateOf(habit.lifeAreaId) }
        var editRecurrenceType by remember { mutableStateOf(habit.recurrenceType) }
        var editWeekMask by remember { mutableStateOf(habit.recurrenceDaysOfWeekMask) }
        var editIntervalText by remember { mutableStateOf(habit.recurrenceInterval.toString()) }
        var editDayOfMonthText by remember { mutableStateOf((habit.recurrenceDayOfMonth ?: 1).toString()) }
        var editWeekOfMonthText by remember { mutableStateOf((habit.recurrenceWeekOfMonth ?: 1).toString()) }
        var editWeekdayText by remember { mutableStateOf((habit.recurrenceWeekday ?: 1).toString()) }
        var editYearlyDates by remember {
            mutableStateOf(
                habit.recurrenceYearlyDates.ifBlank {
                    String.format(java.util.Locale.US, "%02d-%02d", java.time.LocalDate.now().monthValue, java.time.LocalDate.now().dayOfMonth)
                }
            )
        }
        var editStartDate by remember {
            mutableStateOf((habit.recurrenceStartDate ?: java.time.LocalDate.now()).toString())
        }
        var editEndDate by remember { mutableStateOf(habit.recurrenceEndDate?.toString().orEmpty()) }

        val canSave = editName.trim().isNotBlank() &&
            editStartDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) &&
            (editEndDate.isBlank() || editEndDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) &&
            when (editRecurrenceType) {
                HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> editWeekMask != 0
                HabitRecurrenceType.EVERY_N_DAYS -> editIntervalText.toIntOrNull()?.let { it >= 1 } == true
                HabitRecurrenceType.MONTHLY_BY_DATE -> editDayOfMonthText.toIntOrNull() in 1..31
                HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                    val w = editWeekOfMonthText.toIntOrNull()
                    val d = editWeekdayText.toIntOrNull()
                    (w in 1..5 || w == -1) && d in 1..7
                }
                HabitRecurrenceType.YEARLY_BY_DATE,
                HabitRecurrenceType.YEARLY_MULTI_DATE -> editYearlyDates.split(',').any {
                    it.trim().matches(Regex("^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$"))
                }
                HabitRecurrenceType.DAILY -> true
            }

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

                    if (habit.type == HabitType.ROUTINE) {
                        var routineExpanded by remember { mutableStateOf(false) }
                        Text("Routine", style = MaterialTheme.typography.titleSmall)
                        Box {
                            TextButton(onClick = { routineExpanded = true }) {
                                Text("Routine: ${routines.firstOrNull { it.id == editRoutineId }?.name ?: "Select routine"}")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select routine")
                            }
                            DropdownMenu(expanded = routineExpanded, onDismissRequest = { routineExpanded = false }) {
                                routines.forEach { routine ->
                                    DropdownMenuItem(
                                        text = { Text(routine.name) },
                                        onClick = {
                                            routineExpanded = false
                                            editRoutineId = routine.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    var lifeAreaExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { lifeAreaExpanded = true }) {
                            Text("Life Area: ${lifeAreas.find { it.id == editLifeAreaId }?.let { "${it.emoji ?: ""} ${it.name}".trim() } ?: "None"}")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select life area")
                        }
                        DropdownMenu(expanded = lifeAreaExpanded, onDismissRequest = { lifeAreaExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { lifeAreaExpanded = false; editLifeAreaId = null }
                            )
                            lifeAreas.forEach { la ->
                                DropdownMenuItem(
                                    text = { Text("${la.emoji ?: ""} ${la.name}".trim()) },
                                    onClick = { lifeAreaExpanded = false; editLifeAreaId = la.id }
                                )
                            }
                        }
                    }

                    Text("Schedule", style = MaterialTheme.typography.titleSmall)
                    RecurrenceTypeSelector(
                        selectedType = editRecurrenceType,
                        onTypeSelected = { editRecurrenceType = it }
                    )

                    when (editRecurrenceType) {
                        HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> {
                            Text("Days of week")
                            WeekdayMaskEditor(
                                selectedMask = editWeekMask,
                                onToggle = { day ->
                                    val bit = 1 shl day
                                    editWeekMask = if ((editWeekMask and bit) != 0) {
                                        editWeekMask and bit.inv()
                                    } else {
                                        editWeekMask or bit
                                    }
                                }
                            )
                        }
                        HabitRecurrenceType.EVERY_N_DAYS -> {
                            OutlinedTextField(
                                value = editIntervalText,
                                onValueChange = { editIntervalText = it.filter { c -> c.isDigit() } },
                                label = { Text("Every N days") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }
                        HabitRecurrenceType.MONTHLY_BY_DATE -> {
                            OutlinedTextField(
                                value = editDayOfMonthText,
                                onValueChange = { editDayOfMonthText = it.filter { c -> c.isDigit() } },
                                label = { Text("Day of month (1-31)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }
                        HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                            OutlinedTextField(
                                value = editWeekOfMonthText,
                                onValueChange = {
                                    editWeekOfMonthText = it.filter { c -> c.isDigit() || c == '-' }
                                },
                                label = { Text("Week of month (1-5 or -1 for last)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editWeekdayText,
                                onValueChange = { editWeekdayText = it.filter { c -> c.isDigit() } },
                                label = { Text("Weekday (1=Mon ... 7=Sun)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }
                        HabitRecurrenceType.YEARLY_BY_DATE,
                        HabitRecurrenceType.YEARLY_MULTI_DATE -> {
                            OutlinedTextField(
                                value = editYearlyDates,
                                onValueChange = {
                                    editYearlyDates = it
                                        .uppercase()
                                        .filter { c -> c.isDigit() || c == '-' || c == ',' || c == ' ' }
                                },
                                label = { Text("Yearly dates MM-DD (comma separated)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HabitRecurrenceType.DAILY -> Unit
                    }

                    OutlinedTextField(
                        value = editStartDate,
                        onValueChange = { editStartDate = it.trim() },
                        label = { Text("Start date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEndDate,
                        onValueChange = { editEndDate = it.trim() },
                        label = { Text("End date (optional YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateHabitWithRecurrence(
                        habit = habit,
                        newName = editName,
                        newDescription = editDesc,
                        newTarget = editTarget,
                        newOp = editOp,
                        newRoutineId = editRoutineId,
                        newLifeAreaId = editLifeAreaId,
                        recurrenceType = editRecurrenceType,
                        recurrenceDaysOfWeekMask = editWeekMask,
                        recurrenceIntervalText = editIntervalText,
                        recurrenceDayOfMonthText = editDayOfMonthText,
                        recurrenceWeekOfMonthText = editWeekOfMonthText,
                        recurrenceWeekdayText = editWeekdayText,
                        yearlyDatesCsv = editYearlyDates,
                        startDateText = editStartDate,
                        endDateText = editEndDate
                    )
                    editingHabit = null
                }, enabled = canSave) { Text("Save") }
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
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text("Are you sure you want to delete '${habit.name}'?")
                    Text(
                        text = "This cannot be undone and will remove all tracked data for this habit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
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

    if (showHabitTypeInfo) {
        AlertDialog(
            onDismissRequest = { showHabitTypeInfo = false },
            title = { Text("Habit Types Guide") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitType.values().forEach { type ->
                        Text(
                            text = "• ${habitTypeLabel(type)}: ${habitTypeDescription(type)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHabitTypeInfo = false }) { Text("Got it") }
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
                        SectionHeader(
                            title = "Add Habit",
                            subtitle = "Set your minimum — the version you'll do even on your worst day. Required fields are marked with *."
                        )
                        Text(
                            text = "Fields marked * are required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SectionHeader(title = "1. Habit Basics")
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
                            label = { Text("Who I'm becoming *") }
                        )
                        OutlinedTextField(
                            value = viewModel.description,
                            onValueChange = viewModel::updateDescription,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("What this habit means to me *") }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose habit type",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = { showHabitTypeInfo = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Habit type help")
                            }
                        }
                        HabitTypeSelector(
                            selectedType = viewModel.selectedType,
                            onTypeSelected = viewModel::updateType
                        )

                        SectionHeader(
                            title = "2. Recurrence",
                            subtitle = "Set how often this habit should appear"
                        )
                        RecurrenceTypeSelector(
                            selectedType = viewModel.selectedRecurrenceType,
                            onTypeSelected = viewModel::updateRecurrenceType
                        )
                        Text(
                            text = recurrenceDescription(viewModel.selectedRecurrenceType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        when (viewModel.selectedRecurrenceType) {
                            HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> {
                                Text("Days of week")
                                WeekdayMaskEditor(
                                    selectedMask = viewModel.selectedWeekdaysMask,
                                    onToggle = viewModel::toggleWeekday
                                )
                            }

                            HabitRecurrenceType.EVERY_N_DAYS -> {
                                OutlinedTextField(
                                    value = viewModel.everyNDaysInterval,
                                    onValueChange = viewModel::updateEveryNDaysInterval,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Every N days") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                            }

                            HabitRecurrenceType.MONTHLY_BY_DATE -> {
                                OutlinedTextField(
                                    value = viewModel.monthlyDayOfMonth,
                                    onValueChange = viewModel::updateMonthlyDayOfMonth,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Day of month (1-31)") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                                Text(
                                    "Invalid dates are skipped (e.g., 31st in short months).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> {
                                OutlinedTextField(
                                    value = viewModel.monthlyNthWeek,
                                    onValueChange = viewModel::updateMonthlyNthWeek,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Week of month (1-5 or -1 for last)") }
                                )
                                OutlinedTextField(
                                    value = viewModel.monthlyNthWeekday,
                                    onValueChange = viewModel::updateMonthlyNthWeekday,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Weekday (1=Mon ... 7=Sun)") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                            }

                            HabitRecurrenceType.YEARLY_BY_DATE,
                            HabitRecurrenceType.YEARLY_MULTI_DATE -> {
                                OutlinedTextField(
                                    value = viewModel.yearlyDatesCsv,
                                    onValueChange = viewModel::updateYearlyDatesCsv,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Yearly dates MM-DD (comma separated)") },
                                    supportingText = { Text("Example: 01-01,07-01") }
                                )
                            }

                            HabitRecurrenceType.DAILY -> Unit
                        }

                        OutlinedTextField(
                            value = viewModel.recurrenceStartDateText,
                            onValueChange = viewModel::updateRecurrenceStartDateText,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Start date (YYYY-MM-DD)") }
                        )
                        OutlinedTextField(
                            value = viewModel.recurrenceEndDateText,
                            onValueChange = viewModel::updateRecurrenceEndDateText,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("End date (optional YYYY-MM-DD)") }
                        )

                        SectionHeader(
                            title = "3. Type-Specific Fields",
                            subtitle = "Only fields below change based on selected habit type"
                        )

                        if (viewModel.selectedType == HabitType.BOOLEAN || viewModel.selectedType == HabitType.TEXT) {
                            Text(
                                text = "No numeric target required for ${habitTypeLabel(viewModel.selectedType)} habits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (viewModel.selectedType == HabitType.ROUTINE) {
                            var routineExpanded by remember { mutableStateOf(false) }
                            Text(
                                text = "Choose a routine to link",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box {
                                TextButton(onClick = { routineExpanded = true }) {
                                    Text(
                                        "Routine: ${routines.firstOrNull { it.id == viewModel.selectedRoutineId }?.name ?: "Select routine"}"
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select routine")
                                }
                                DropdownMenu(expanded = routineExpanded, onDismissRequest = { routineExpanded = false }) {
                                    routines.forEach { routine ->
                                        DropdownMenuItem(
                                            text = { Text(routine.name) },
                                            onClick = {
                                                routineExpanded = false
                                                viewModel.updateSelectedRoutine(routine.id)
                                            }
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Completing this routine will automatically mark the habit done.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Life area selector
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

                        SectionHeader(
                            title = "4. Common Fields",
                            subtitle = "These apply to all habit types"
                        )

                        SectionHeader(
                            title = "Commitment Reminder",
                            subtitle = "Set when and where you commit to this habit"
                        )
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
                                label = { Text("Minimum commitment (minutes)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                supportingText = { Text("Start small — what's the least you'd commit to each day?") }
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
                                label = { Text("Daily commitment (optional)") },
                                supportingText = if (viewModel.targetValue.isNotBlank()) {
                                    {
                                        Text(
                                            text = "Set this to your minimum — the amount you'll do even on hard days. You'll almost always exceed it.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
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
                        lifeAreaName = lifeAreas.find { it.id == habit.lifeAreaId }?.let { "${it.emoji ?: ""} ${it.name}".trim() },
                        onEdit = { editingHabit = habit },
                        onDelete = { habitToDelete = habit },
                        onLifecycleChange = { status -> viewModel.setHabitLifecycle(habit, status) }
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
            Text("Type: ${habitTypeLabel(selectedType)}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select habit type")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HabitType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(habitTypeLabel(type)) },
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
    lifeAreaName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLifecycleChange: (HabitLifecycleStatus) -> Unit
) {
    var lifecycleMenuExpanded by remember { mutableStateOf(false) }

    val targetLabel = habit.targetValue?.let {
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
            "$opLabel %02d:%02d $amPm".format(h12, m)
        } else {
            "$opLabel $it ${habit.unit ?: ""}".trim()
        }
    }

    val attributes = buildList {
        add("Type" to habit.type.name)
        add("Recurs" to recurrenceLabel(habit.recurrenceType))
        habit.unit?.takeIf { it.isNotBlank() }?.let { add("Unit" to it) }
        targetLabel?.let { add("Target" to it) }
        lifeAreaName?.let { add("Life Area" to it) }
        add("Status" to habit.lifecycleStatus.label)
    }

    LeafSectionItemCard(
        title = habit.name,
        subtitle = habit.description.takeIf { it.isNotBlank() },
        attributes = attributes,
        trailingActions = {
            Box {
                TextButton(onClick = { lifecycleMenuExpanded = true }) {
                    Text(
                        habit.lifecycleStatus.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Change lifecycle status")
                }
                DropdownMenu(
                    expanded = lifecycleMenuExpanded,
                    onDismissRequest = { lifecycleMenuExpanded = false }
                ) {
                    HabitLifecycleStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(status.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        status.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                lifecycleMenuExpanded = false
                                onLifecycleChange(status)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun RecurrenceTypeSelector(
    selectedType: HabitRecurrenceType,
    onTypeSelected: (HabitRecurrenceType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Recurrence: ${recurrenceLabel(selectedType)}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select recurrence")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HabitRecurrenceType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(recurrenceLabel(type)) },
                    onClick = {
                        expanded = false
                        onTypeSelected(type)
                    }
                )
            }
        }
    }
}

private fun habitTypeLabel(type: HabitType): String = when (type) {
    HabitType.BOOLEAN -> "Checkbox (Done/Not Done)"
    HabitType.NUMBER -> "Number"
    HabitType.DURATION -> "Duration"
    HabitType.COUNT -> "Count"
    HabitType.POMODORO -> "Pomodoro Sessions"
    HabitType.TIMER -> "Timer Minutes"
    HabitType.TIME -> "Time of Day"
    HabitType.TEXT -> "Text / Notes"
    HabitType.ROUTINE -> "Routine"
}

private fun habitTypeDescription(type: HabitType): String = when (type) {
    HabitType.BOOLEAN -> "Simple check-in. Set the minimum so low it's impossible to skip. Example: Did I stretch at all today?"
    HabitType.NUMBER -> "Track a numeric value. Set your floor, not your ceiling. Example: Water = 4 glasses (minimum)"
    HabitType.DURATION -> "Track time spent. Start tiny — showing up for 5 min beats skipping 20 min. Example: Meditation = 5 min"
    HabitType.COUNT -> "Track integer count. Make the minimum laughably small. Example: Push-ups = 5"
    HabitType.POMODORO -> "Track focus sessions. One session a day builds the habit. Example: 1 Pomodoro (minimum)"
    HabitType.TIMER -> "Track a timed session in minutes. Example: Deep work = 15 min (your floor)"
    HabitType.TIME -> "Track timing relative to a target time. Example: In bed by 11 PM"
    HabitType.TEXT -> "Capture a note — any note. One sentence counts. Example: Journal reflection"
    HabitType.ROUTINE -> "Execute a pre-configured routine of exercises. Example: Morning movement routine"
}

private fun recurrenceLabel(type: HabitRecurrenceType): String = when (type) {
    HabitRecurrenceType.DAILY -> "Daily"
    HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> "Weekly (Selected Days)"
    HabitRecurrenceType.EVERY_N_DAYS -> "Every N Days"
    HabitRecurrenceType.MONTHLY_BY_DATE -> "Monthly (Date)"
    HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> "Monthly (Nth Weekday)"
    HabitRecurrenceType.YEARLY_BY_DATE -> "Yearly (Single Date)"
    HabitRecurrenceType.YEARLY_MULTI_DATE -> "Yearly (Multiple Dates)"
}

private fun recurrenceDescription(type: HabitRecurrenceType): String = when (type) {
    HabitRecurrenceType.DAILY -> "Appears every day."
    HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> "Choose exact weekdays (e.g., Mon, Wed, Fri)."
    HabitRecurrenceType.EVERY_N_DAYS -> "Repeats based on a day interval from start date."
    HabitRecurrenceType.MONTHLY_BY_DATE -> "Runs on a fixed date each month (e.g., 10th)."
    HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> "Runs on patterns like 2nd Tuesday or last Friday."
    HabitRecurrenceType.YEARLY_BY_DATE -> "Runs once per year on one date."
    HabitRecurrenceType.YEARLY_MULTI_DATE -> "Runs on multiple dates each year (MM-DD list)."
}

@Composable
private fun WeekdayMaskEditor(
    selectedMask: Int,
    onToggle: (Int) -> Unit
) {
    val days = listOf(
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { (day, label) ->
            val bit = 1 shl day.value
            val selected = (selectedMask and bit) != 0
            TextButton(onClick = { onToggle(day.value) }) {
                Text(if (selected) "[$label]" else label)
            }
        }
    }
}
