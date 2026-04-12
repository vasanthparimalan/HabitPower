package com.example.habitpower.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.gamification.GamificationViewModel
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.theme.AppSpacing
import com.example.habitpower.ui.theme.CompletionPulse
import com.example.habitpower.ui.theme.DayCompletionKick
import com.example.habitpower.ui.theme.GamificationSummaryCard
import com.example.habitpower.ui.theme.SectionHeader
import com.example.habitpower.ui.theme.StatusChip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val HOLD_TO_COMPLETE_MS = 760

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
    gamificationViewModel: GamificationViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigate: (String) -> Unit = {}
) {
    val heatmapData by viewModel.heatmapData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sessionsForSelectedDate by viewModel.sessionsForSelectedDate.collectAsState()
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val selectedDateHabits by viewModel.selectedDateHabits.collectAsState()
    val todayHabits by viewModel.todayHabits.collectAsState()
    val missedYesterday by viewModel.missedYesterdayHabitIds.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val atomicQuotes by viewModel.atomicQuotes.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val thisWeekConsistency by viewModel.thisWeekConsistency.collectAsState()
    val bestPersonalRecord by viewModel.bestPersonalRecord.collectAsState()
    val lifeAreaCompletion by viewModel.lifeAreaCompletionForSelectedDate.collectAsState()
    val gamificationState by gamificationViewModel.uiState.collectAsState()

    var showQuotesDialog by remember { mutableStateOf(false) }
    var showGamificationInfoDialog by remember { mutableStateOf(false) }

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

    if (showGamificationInfoDialog) {
        AlertDialog(
            onDismissRequest = { showGamificationInfoDialog = false },
            title = { Text("How Gamification Works") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Complete habits to earn XP and build consistency.")
                    Text("• Perfect daily check-ins keep your streak alive.")
                    Text("• Levels increase as XP grows, unlocking stronger momentum.")
                    Text("• Track streak, level progress, and achievements from this Dashboard.")
                    Text("• For full family workflow guidance, open Guide & Help.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showGamificationInfoDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGamificationInfoDialog = false
                    onNavigate(Screen.Help.route)
                }) {
                    Text("Open Guide")
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
                            style = MaterialTheme.typography.titleLarge
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
                    IconButton(onClick = { onNavigate(Screen.Help.route) }) {
                        Icon(Icons.Default.Info, contentDescription = "Help")
                    }
                    IconButton(onClick = { showQuotesDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Atomic principles")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, selectedDate)) }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChip(
                            text = "Daily Cue",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Becoming 1% better every day.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "\"$dailyQuote\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            // ── Gamification: streak + XP bar ────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gamification Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { showGamificationInfoDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "How gamification works")
                        }
                    }
                    GamificationSummaryCard(
                        streak = gamificationState.streak,
                        level = gamificationState.level,
                        levelName = gamificationState.levelName,
                        levelProgress = gamificationState.levelProgress,
                        xpLabel = gamificationState.xpLabel
                    )
                }
            }
            item {
                SectionHeader(
                    title = "Last 3 Months Habits",
                    subtitle = "Consistency overview by day"
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
                WeeklyReviewCard(
                    currentStreak = currentStreak,
                    thisWeekConsistency = thisWeekConsistency,
                    bestPersonalRecord = bestPersonalRecord
                )
            }

            item {
                SectionHeader(
                    title = "Recent Days",
                    subtitle = "Review today and backfill up to last 3 days"
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Last7DaysSelector(
                    selectedDate = selectedDate,
                    onSelectDate = viewModel::selectDate
                )
            }

            if (selectedDateHabits.isNotEmpty()) {
                item {
                    val missed = selectedDateHabits.filter { missedYesterday.contains(it.habitId) && !isCompleted(it) }

                    if (selectedDate == LocalDate.now() && missed.isNotEmpty()) {
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

                    Text(
                        "Habits - ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd"))}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val uncompleted = selectedDateHabits.filter { !isCompleted(it) }
                val completed = selectedDateHabits.filter { isCompleted(it) }
                val allSorted = uncompleted + completed
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayCompletionKick(
                            allDone = completed.isNotEmpty() && completed.size == selectedDateHabits.size,
                            completed = completed.size,
                            total = selectedDateHabits.size,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                                val statusText = when {
                                                    isDone -> "Completed"
                                                    isMissed -> "Missed yesterday"
                                                    else -> "Pending"
                                                }
                                                Text(
                                                    text = statusText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (!isDone && habit.type != HabitType.BOOLEAN && habit.type != HabitType.POMODORO && habit.type != HabitType.TIMER) {
                                                    TextButton(
                                                        onClick = { viewModel.logTwoMinutes(habit, selectedDate) },
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
                                                    onCheckedChange = { viewModel.toggleHabit(habit.habitId, selectedDate) }
                                                )
                                            } else {
                                                if (isDone) {
                                                    CompletionPulse(visible = true)
                                                } else {
                                                    IconButton(onClick = {
                                                        if ((habit.type == HabitType.POMODORO || habit.type == HabitType.TIMER) && selectedDate == LocalDate.now()) {
                                                            onNavigate(Screen.Focus.createRoute(habit.habitId))
                                                        } else {
                                                            onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, selectedDate))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }

            item {
                SectionHeader(
                    title = "Today Tasks",
                    subtitle = "Simple bottom list of what still needs to be done"
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                val todayPending = todayHabits.filter { !isCompleted(it) }
                val todayDone = todayHabits.filter { isCompleted(it) }

                if (todayHabits.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No tasks for today. Assign habits or apply a starter stack from Admin.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    StatusChip(text = "${todayDone.size}/${todayHabits.size} done")
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    todayPending.forEach { habit ->
                        TodayTaskRow(
                            habit = habit,
                            onComplete = {
                                if (habit.type == HabitType.BOOLEAN) {
                                    viewModel.toggleHabit(habit.habitId, LocalDate.now())
                                } else {
                                    onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now()))
                                }
                            },
                            onOpen = {
                                if (habit.type == HabitType.POMODORO || habit.type == HabitType.TIMER) {
                                    onNavigate(Screen.Focus.createRoute(habit.habitId))
                                } else {
                                    onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now()))
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (todayDone.size == todayHabits.size) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = "Life areas complete for today. Keep this rhythm tomorrow as well.",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Last7DaysSelector(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = (0L..2L).map { today.minusDays(it) }.reversed()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { date ->
            val selected = date == selectedDate
            Card(
                modifier = Modifier.border(
                    width = if (selected) 1.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = MaterialTheme.shapes.medium
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { onSelectDate(date) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (date == today) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (selected) {
                        StatusChip(
                            text = "Selected",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(top = AppSpacing.xs)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyReviewCard(
    currentStreak: Int,
    thisWeekConsistency: Int,
    bestPersonalRecord: Int
) {
    val improvement = when {
        thisWeekConsistency < 40 -> "Focus on one non-negotiable habit this week."
        thisWeekConsistency < 70 -> "You are building momentum. Protect your current streak."
        else -> "Great consistency. Keep improving one weak life area."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader(title = "Weekly Review", subtitle = "Quick reflection with zero clutter")
            Text("Current streak: $currentStreak days", style = MaterialTheme.typography.bodySmall)
            Text("This week consistency: $thisWeekConsistency%", style = MaterialTheme.typography.bodySmall)
            Text("Best week record: $bestPersonalRecord%", style = MaterialTheme.typography.bodySmall)
            StatusChip(
                text = if (thisWeekConsistency >= 70) "Momentum strong" else "Keep one anchor habit",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(improvement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayTaskRow(
    habit: DailyHabitItem,
    onComplete: () -> Unit,
    onOpen: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = habit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (habit.type) {
                        HabitType.BOOLEAN -> "Hold to complete"
                        HabitType.POMODORO, HabitType.TIMER -> "Open focus"
                        else -> "Log progress"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (habit.type == HabitType.BOOLEAN) {
                HoldToCompleteButton(onComplete = onComplete)
            } else {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpen()
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .widthIn(min = 88.dp)
                ) { Text("Open") }
            }
        }
    }
}

@Composable
private fun HoldToCompleteButton(onComplete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var firedForCurrentPress by remember { mutableStateOf(false) }
    val holdProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = HOLD_TO_COMPLETE_MS, easing = LinearEasing),
        label = "holdProgress"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "holdScale"
    )

    LaunchedEffect(isPressed, holdProgress) {
        if (!isPressed) {
            firedForCurrentPress = false
            return@LaunchedEffect
        }
        if (!firedForCurrentPress && holdProgress >= 0.99f) {
            firedForCurrentPress = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .widthIn(min = 88.dp)
            .heightIn(min = 48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = {})
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.small)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(holdProgress.coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), MaterialTheme.shapes.small)
                    .align(Alignment.CenterStart)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isPressed) "Hold..." else "Hold")
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
        Color(0xFF0072B2),
        Color(0xFFE69F00),
        Color(0xFF009E73),
        Color(0xFF56B4E9),
        Color(0xFFD55E00),
        Color(0xFFCC79A7),
        Color(0xFF1A73B8),
        Color(0xFF4E7A5D)
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
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    when {
                        ratio >= 1f -> MaterialTheme.colorScheme.primary
                        ratio >= 0.75f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        ratio >= 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        ratio > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
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
