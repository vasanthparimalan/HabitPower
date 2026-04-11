package com.example.habitpower.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DailyCheckInScreen(
    navigateBack: () -> Unit,
    viewModel: DailyCheckInViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.userName?.let { "$it Daily Check-In" } ?: "Daily Check-In")
                        Text(
                            text = uiState.date.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
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
                    if (uiState.userId != null) {
                        viewModel.saveDailyCheckIn(navigateBack)
                    }
                }
            ) {
                Text("Save")
            }
        }
    ) { innerPadding ->
        when {
            uiState.userId == null && !uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No user selected yet.")
                }
            }

            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Loading habits...")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Completed ${uiState.completedCount} of ${uiState.totalCount} habits",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (uiState.habits.isEmpty()) {
                        item {
                            Text("No habits are assigned to this user yet.")
                        }
                    } else {
                        items(uiState.habits, key = { it.habitId }) { habit ->
                            HabitInputCard(
                                habit = habit,
                                onTextChange = { viewModel.updateTextValue(habit.habitId, it) },
                                onBooleanChange = { viewModel.updateBooleanValue(habit.habitId, it) },
                                onTimeChange = { display, minFromNoon ->
                                    viewModel.updateTimeValue(habit.habitId, display, minFromNoon)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitInputCard(
    habit: DailyCheckInHabitInput,
    onTextChange: (String) -> Unit,
    onBooleanChange: (Boolean) -> Unit,
    onTimeChange: (display: String, minutesFromNoon: Double) -> Unit = { _, _ -> }
) {
    var showTimeCalculator by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(habit.name, style = MaterialTheme.typography.titleMedium)
            if (habit.description.isNotBlank()) {
                Text(habit.description, style = MaterialTheme.typography.bodyMedium)
            }

            when (habit.type) {
                HabitType.BOOLEAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Completed")
                        Switch(
                            checked = habit.booleanValue,
                            onCheckedChange = onBooleanChange
                        )
                    }
                }

                HabitType.NUMBER, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> {
                    OutlinedTextField(
                        value = habit.textValue,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            val unitLabel = habit.unit?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                            Text("Enter value$unitLabel")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (habit.type == HabitType.COUNT || habit.type == HabitType.POMODORO || habit.type == HabitType.TIMER) KeyboardType.Number else KeyboardType.Decimal
                        )
                    )
                }

                HabitType.DURATION -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = habit.textValue,
                            onValueChange = onTextChange,
                            modifier = Modifier.weight(1f),
                            label = {
                                val unitLabel = habit.unit?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                                Text("Enter value$unitLabel")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        IconButton(onClick = { showTimeCalculator = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Calculate Time")
                        }
                    }
                }

                HabitType.TEXT -> {
                    OutlinedTextField(
                        value = habit.textValue,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Add note") },
                        minLines = 3
                    )
                }

                HabitType.TIME -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val label = if (habit.textValue.isBlank()) "Tap to select time" else habit.textValue
                    Button(
                        onClick = {
                            // Pre-fill with current time
                            val now = java.time.LocalTime.now()
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    val isPM = h >= 12
                                    val h12 = if (h % 12 == 0) 12 else h % 12
                                    val display = "%02d:%02d %s".format(h12, m, if (isPM) "PM" else "AM")
                                    val minsFromNoon = ((h * 60 + m - 12 * 60 + 24 * 60) % (24 * 60)).toDouble()
                                    onTimeChange(display, minsFromNoon)
                                },
                                now.hour,
                                now.minute,
                                false
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🕐  $label") }
                }
            }

            habit.targetValue?.let { target ->
                val targetLabel = buildString {
                    append("Target: ")
                    if (habit.type == HabitType.COUNT) {
                        append(target.toInt())
                    } else {
                        append(target)
                    }
                    habit.unit?.takeIf { it.isNotBlank() }?.let {
                        append(" ")
                        append(it)
                    }
                }
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (habit.targetMet) {
                        true -> Color(0xFF2E7D32)
                        false, null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }

    if (showTimeCalculator) {
        TimeCalculatorDialog(
            onDismiss = { showTimeCalculator = false },
            onCalculated = { hours ->
                onTextChange(String.format(java.util.Locale.US, "%.2f", hours))
            }
        )
    }
}

@Composable
fun TimeCalculatorDialog(
    onDismiss: () -> Unit,
    onCalculated: (Double) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var startHour by remember { mutableStateOf<Int?>(null) }
    var startMinute by remember { mutableStateOf<Int?>(null) }
    var endHour by remember { mutableStateOf<Int?>(null) }
    var endMinute by remember { mutableStateOf<Int?>(null) }

    fun formatTime(hour: Int?, min: Int?): String {
        if (hour == null || min == null) return "Select Time"
        val isPM = hour >= 12
        val h = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (isPM) "PM" else "AM"
        return String.format(java.util.Locale.US, "%02d:%02d %s", h, min, amPm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculate Duration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Start Time:")
                Button(onClick = {
                    android.app.TimePickerDialog(context, { _, h, m ->
                        startHour = h
                        startMinute = m
                    }, startHour ?: 22, startMinute ?: 0, false).show()
                }) { Text(formatTime(startHour, startMinute)) }

                Text("End Time:")
                Button(onClick = {
                    android.app.TimePickerDialog(context, { _, h, m ->
                        endHour = h
                        endMinute = m
                    }, endHour ?: 6, endMinute ?: 0, false).show()
                }) { Text(formatTime(endHour, endMinute)) }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (startHour != null && endHour != null) {
                        val startMinTotal = startHour!! * 60 + startMinute!!
                        var endMinTotal = endHour!! * 60 + endMinute!!
                        if (endMinTotal < startMinTotal) {
                            endMinTotal += 24 * 60
                        }
                        val diff = endMinTotal - startMinTotal
                        onCalculated(diff / 60.0)
                        onDismiss()
                    }
                },
                enabled = startHour != null && endHour != null
            ) { Text("Calculate") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
