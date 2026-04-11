package com.example.habitpower.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.navigation.Screen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigate: (String) -> Unit = {}
) {
    val heatmapData by viewModel.heatmapData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sessionsForSelectedDate by viewModel.sessionsForSelectedDate.collectAsState()
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val todayHabits by viewModel.todayHabits.collectAsState()
    val missedYesterday by viewModel.missedYesterdayHabitIds.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val atomicQuotes by viewModel.atomicQuotes.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val thisWeekConsistency by viewModel.thisWeekConsistency.collectAsState()
    val bestPersonalRecord by viewModel.bestPersonalRecord.collectAsState()
    val lifeAreaCompletion by viewModel.lifeAreaCompletionForSelectedDate.collectAsState()

    var showQuotesDialog by remember { mutableStateOf(false) }

    if (showQuotesDialog) {
        AlertDialog(
            onDismissRequest = { showQuotesDialog = false },
            title = { Text("Atomic Principles") },
            text = {
                LazyColumn {
                    items(atomicQuotes.size) { i ->
                        Text(
                            text = "• ${atomicQuotes[i].text}",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuotesDialog = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuotesDialog = false
                    onNavigate("admin/quotes")
                }) {
                    Text("Edit Quotes")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = activeUser?.name ?: "No user",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    if (users.isNotEmpty()) {
                        UserSelectorMenu(
                            users = users,
                            activeUserId = activeUser?.id,
                            onSelectUser = viewModel::setActiveUser
                        )
                    }
                    TextButton(onClick = { onNavigate(Screen.AdminHome.route) }) {
                        Text("Admin")
                    }
                    IconButton(onClick = { showQuotesDialog = true }) {
                        Text("📖")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id)) }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Check-In")
                Spacer(Modifier.width(8.dp))
                Text("Log Advanced")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Becoming 1% better every day.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$dailyQuote\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                DashboardKpiAndLifeAreaSection(
                    currentStreak = currentStreak,
                    thisWeekConsistency = thisWeekConsistency,
                    bestPersonalRecord = bestPersonalRecord,
                    lifeAreaCompletion = lifeAreaCompletion,
                    selectedDate = selectedDate
                )
            }

            item {
                Text(
                    text = "Last 3 Months Habits",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MonthlyHeatmap(
                        yearMonth = YearMonth.now().minusMonths(2),
                        heatmapData = heatmapData
                    )
                    MonthlyHeatmap(
                        yearMonth = YearMonth.now().minusMonths(1),
                        heatmapData = heatmapData
                    )
                    MonthlyHeatmap(
                        yearMonth = YearMonth.now(),
                        heatmapData = heatmapData
                    )
                }
            }

            if (selectedDate == LocalDate.now() && todayHabits.isNotEmpty()) {
                item {
                    val missed = todayHabits.filter { missedYesterday.contains(it.habitId) && !isCompleted(it) }

                    if (missed.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "You missed ${missed.size} habits yesterday. Don't break the chain twice!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Text("Today's Habits", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val uncompleted = todayHabits.filter { !isCompleted(it) }
                val completed = todayHabits.filter { isCompleted(it) }
                val allSorted = uncompleted + completed
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allSorted.chunked(2).forEach { rowHabits ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowHabits.forEach { habit ->
                                    val isDone = isCompleted(habit)
                                    val isMissed = missedYesterday.contains(habit.habitId) && !isDone

                                    Card(
                                        modifier = Modifier.weight(1f).height(64.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDone) {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            } else if (isMissed) {
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = habit.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold,
                                                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                if (!isDone && habit.type != HabitType.BOOLEAN && habit.type != HabitType.POMODORO && habit.type != HabitType.TIMER) {
                                                    TextButton(
                                                        onClick = { viewModel.logTwoMinutes(habit) },
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.height(20.dp)
                                                    ) {
                                                        Text("Do 2 Mins", fontSize = 10.sp)
                                                    }
                                                }
                                            }

                                            if (habit.type == HabitType.BOOLEAN) {
                                                Checkbox(
                                                    checked = isDone,
                                                    onCheckedChange = { viewModel.toggleHabit(habit.habitId) }
                                                )
                                            } else {
                                                if (isDone) {
                                                    Icon(Icons.Default.Check, "Done", tint = MaterialTheme.colorScheme.primary)
                                                } else {
                                                    IconButton(onClick = {
                                                        if (habit.type == HabitType.POMODORO || habit.type == HabitType.TIMER) {
                                                            onNavigate(Screen.Focus.createRoute(habit.habitId))
                                                        } else {
                                                            onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id))
                                                        }
                                                    }) {
                                                        Icon(if (habit.type == HabitType.POMODORO || habit.type == HabitType.TIMER) Icons.Default.PlayArrow else Icons.Default.Edit, "Log")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (rowHabits.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (sessionsForSelectedDate.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Completed Routines - ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(sessionsForSelectedDate.size) { index ->
                    val session = sessionsForSelectedDate[index]
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.routineName, style = MaterialTheme.typography.labelLarge)
                                Text("Completed: ${session.date}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "No specific routines completed on ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DashboardKpiAndLifeAreaSection(
    currentStreak: Int,
    thisWeekConsistency: Int,
    bestPersonalRecord: Int,
    lifeAreaCompletion: List<LifeAreaCompletion>,
    selectedDate: LocalDate
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KPICard(
                label = "Streak",
                value = currentStreak.toString(),
                unit = "days",
                modifier = Modifier.weight(1f)
            )
            KPICard(
                label = "This Week",
                value = thisWeekConsistency.toString(),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
            KPICard(
                label = "Best",
                value = bestPersonalRecord.toString(),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Life Area Completion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = selectedDate.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LifeAreaRoseChart(
                    data = lifeAreaCompletion,
                    holeColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(170.dp)
                )

                val activeSegments = lifeAreaCompletion.filter { it.totalCount > 0 }
                Text(
                    text = if (activeSegments.isEmpty()) {
                        "No assigned life-area habits on this date"
                    } else {
                        "${activeSegments.sumOf { it.completedCount }} / ${activeSegments.sumOf { it.totalCount }} completed"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LifeAreaRoseChart(
    data: List<LifeAreaCompletion>,
    holeColor: Color,
    modifier: Modifier = Modifier
) {
    val segments = data.filter { it.totalCount > 0 }
    val palette = listOf(
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFFE91E63),
        Color(0xFF009688),
        Color(0xFF795548),
        Color(0xFF3F51B5)
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (segments.isEmpty()) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gapDegrees = 4f
                val segmentSweep = (360f - (segments.size * gapDegrees)) / segments.size
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val innerRadius = maxRadius * 0.34f

                segments.forEachIndexed { index, segment ->
                    val start = -90f + index * (segmentSweep + gapDegrees)
                    val color = palette[index % palette.size]
                    val completionRatio = segment.completionPercent.coerceIn(0f, 100f) / 100f
                    val petalRadius = innerRadius + ((maxRadius - innerRadius) * completionRatio)
                    val backgroundTopLeft = Offset(
                        x = center.x - maxRadius,
                        y = center.y - maxRadius
                    )
                    val petalTopLeft = Offset(
                        x = center.x - petalRadius,
                        y = center.y - petalRadius
                    )

                    drawArc(
                        color = color.copy(alpha = 0.22f),
                        startAngle = start,
                        sweepAngle = segmentSweep,
                        useCenter = true,
                        topLeft = backgroundTopLeft,
                        size = Size(maxRadius * 2f, maxRadius * 2f)
                    )
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = segmentSweep,
                        useCenter = true,
                        topLeft = petalTopLeft,
                        size = Size(petalRadius * 2f, petalRadius * 2f)
                    )
                }

                drawCircle(
                    color = holeColor,
                    radius = innerRadius * 0.82f,
                    center = center
                )
            }

            val totalCount = segments.sumOf { it.totalCount }
            val completedCount = segments.sumOf { it.completedCount }
            val overallPercent = if (totalCount == 0) 0 else (completedCount * 100 / totalCount)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$overallPercent%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "overall",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isCompleted(habit: DailyHabitItem): Boolean {
    return when (habit.type) {
        HabitType.BOOLEAN -> habit.entryBooleanValue == true
        HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> habit.entryNumericValue != null
        HabitType.TIME -> habit.entryNumericValue != null
        HabitType.TEXT -> !habit.entryTextValue.isNullOrBlank()
    }
}

@Composable
private fun UserSelectorMenu(
    users: List<UserProfile>,
    activeUserId: Long?,
    onSelectUser: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(users.firstOrNull { it.id == activeUserId }?.name ?: "User")
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
private fun MonthlyHeatmap(
    yearMonth: YearMonth,
    heatmapData: Map<LocalDate, Pair<Float, Boolean>>
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstSunday = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val totalDays = ChronoUnit.DAYS.between(firstSunday, lastDayOfMonth).toInt() + 1
    val weeks = (totalDays + 6) / 7

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .width((7 * 14 + 6 * 2).dp) // 7 squares + 6 gaps
                .height((weeks * 14 + (weeks - 1) * 2).dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(totalDays) { index ->
                val date = firstSunday.plusDays(index.toLong())
                val isInMonth = !date.isBefore(firstDayOfMonth) && !date.isAfter(lastDayOfMonth)
                val data = if (isInMonth) heatmapData[date] else null
                val (ratio, hasHabits) = data ?: Pair(0f, false)
                val color = if (!isInMonth) {
                    Color.Transparent
                } else if (!hasHabits) {
                    Color.LightGray.copy(alpha = 0.3f)
                } else {
                    when {
                        ratio >= 1f -> Color(0xFF4CAF50)
                        ratio >= 0.75f -> Color(0xFF81C784)
                        ratio >= 0.5f -> Color(0xFFA5D6A7)
                        ratio > 0f -> Color(0xFFC8E6C9)
                        else -> Color.LightGray.copy(alpha = 0.5f)
                    }
                }

                Card(
                    modifier = Modifier.size(12.dp),
                    colors = CardDefaults.cardColors(containerColor = color),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun KPICard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
