package com.example.habitpower.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.habitpower.ui.theme.GamificationSummaryCard
import com.example.habitpower.ui.theme.SectionHeader
import com.example.habitpower.ui.theme.StatusChip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
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
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val todayHabits by viewModel.todayHabits.collectAsState()
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
                    title = "Daily Check-In",
                    subtitle = "Use one place for all detailed logging and backfill edits"
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                FilledTonalButton(
                    onClick = { onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now())) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text("Open Daily Check-In")
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                SectionHeader(
                    title = "Today Tasks",
                    subtitle = "Sorted by scheduled time — tap \uD83D\uDD52 to set a time, hold to complete"
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                fun sortByTime(list: List<DailyHabitItem>): List<DailyHabitItem> =
                    list.sortedWith(compareBy(nullsLast()) { habit ->
                        habit.commitmentTime?.takeIf { it.isNotBlank() }
                            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    })

                val sortedPending = sortByTime(todayHabits.filter { !isCompleted(it) })
                val sortedDone = sortByTime(todayHabits.filter { isCompleted(it) })

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(text = "${sortedDone.size}/${todayHabits.size} done")
                        if (sortedDone.size == todayHabits.size && todayHabits.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "All done!",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    sortedPending.forEach { habit ->
                        TodayTaskRow(
                            habit = habit,
                            isDone = false,
                            onComplete = {
                                if (habit.type == HabitType.BOOLEAN || habit.type == HabitType.ROUTINE) {
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
                            },
                            onEditTime = { newTime -> viewModel.updateHabitTime(habit.habitId, newTime) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (sortedDone.isNotEmpty()) {
                        if (sortedPending.isNotEmpty()) {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        sortedDone.forEach { habit ->
                            TodayTaskRow(
                                habit = habit,
                                isDone = true,
                                onComplete = { viewModel.toggleHabit(habit.habitId, LocalDate.now()) },
                                onOpen = { onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now())) },
                                onEditTime = { newTime -> viewModel.updateHabitTime(habit.habitId, newTime) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (sortedDone.size == todayHabits.size) {
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
private fun WeeklyReviewCard(
    currentStreak: Int,
    thisWeekConsistency: Int,
    bestPersonalRecord: Int
) {
    val (momentumIcon, momentumLabel) = when {
        thisWeekConsistency < 40 -> Pair(Icons.AutoMirrored.Filled.TrendingUp, "Building")
        thisWeekConsistency < 70 -> Pair(Icons.AutoMirrored.Filled.TrendingUp, "Growing")
        else -> Pair(Icons.Default.LocalFireDepartment, "Strong")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "Weekly Review", subtitle = "Your momentum at a glance")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    icon = Icons.Default.LocalFireDepartment,
                    value = currentStreak.toString(),
                    label = "Day Streak",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Default.BarChart,
                    value = "$thisWeekConsistency%",
                    label = "This Week",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Default.EmojiEvents,
                    value = "$bestPersonalRecord%",
                    label = "Best",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = momentumIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Momentum $momentumLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TodayTaskRow(
    habit: DailyHabitItem,
    isDone: Boolean,
    onComplete: () -> Unit,
    onOpen: () -> Unit,
    onEditTime: (String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val title = habit.name.ifBlank { "Untitled habit" }
    val subtitle = todayTaskSubtitle(habit)
    var showEditTimeDialog by remember { mutableStateOf(false) }

    if (showEditTimeDialog) {
        EditTimeDialog(
            currentTime = habit.commitmentTime.orEmpty(),
            onConfirm = { newTime ->
                onEditTime(newTime.ifBlank { null })
                showEditTimeDialog = false
            },
            onDismiss = { showEditTimeDialog = false }
        )
    }

    val cardColors = if (isDone) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    } else {
        CardDefaults.cardColors()
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Time / location strip ──────────────────────────────────────
            val hasTime = !habit.commitmentTime.isNullOrBlank()
            val hasPlace = habit.commitmentLocation.isNotBlank()
            if (hasTime || hasPlace) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasTime) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = habit.commitmentTime!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (hasPlace) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = habit.commitmentLocation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ── Main row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed badge (only for done tasks)
                if (isDone) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (!isDone) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Edit scheduled time
                IconButton(
                    onClick = { showEditTimeDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Set scheduled time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Action area
                if (isDone) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .widthIn(min = 80.dp)
                            .padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (habit.type == HabitType.BOOLEAN || habit.type == HabitType.ROUTINE) {
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
}

@Composable
private fun EditTimeDialog(
    currentTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var timeInput by remember { mutableStateOf(currentTime) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scheduled Execution Time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Set when this habit is scheduled (24-hour HH:mm). Leave blank to clear.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it; isError = false },
                    label = { Text("Time (HH:mm)") },
                    placeholder = { Text("e.g. 07:30") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Enter a valid time, e.g. 07:30", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = timeInput.trim()
                if (trimmed.isBlank()) {
                    onConfirm("")
                } else {
                    val valid = runCatching { LocalTime.parse(trimmed) }.isSuccess
                    if (valid) onConfirm(trimmed) else isError = true
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
            .width(104.dp)
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

private fun todayTaskSubtitle(habit: DailyHabitItem): String {
    val description = habit.description.trim()
    return when (habit.type) {
        HabitType.BOOLEAN -> if (description.isNotBlank()) description else "Hold to complete"
        HabitType.ROUTINE -> if (description.isNotBlank()) description else "Run linked routine"
        HabitType.POMODORO, HabitType.TIMER -> if (description.isNotBlank()) description else "Open focus session"
        HabitType.COUNT, HabitType.NUMBER, HabitType.DURATION -> {
            if (habit.targetValue != null) {
                val unit = habit.unit?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                "Target ${habit.targetValue}$unit"
            } else if (description.isNotBlank()) {
                description
            } else {
                "Log progress"
            }
        }

        HabitType.TIME -> if (description.isNotBlank()) description else "Log time"
        HabitType.TEXT -> if (description.isNotBlank()) description else "Add note"
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
    var showChartDetailModal by remember { mutableStateOf(false) }
    
    if (showChartDetailModal) {
        ChartDetailModal(
            data = lifeAreaCompletion,
            onClose = { showChartDetailModal = false }
        )
    }

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

                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clickable { showChartDetailModal = true }
                ) {
                    LifeAreaRoseChart(
                        data = lifeAreaCompletion,
                        holeColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    val activeSegments = lifeAreaCompletion.filter { it.totalCount > 0 }
                    val isAllComplete = activeSegments.isNotEmpty() && 
                        activeSegments.all { it.completionPercent >= 100f }
                    
                    if (isAllComplete) {
                        PerfectBalanceAnimation(modifier = Modifier.fillMaxSize())
                    }
                }

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
                
                if (activeSegments.isNotEmpty()) {
                    Text(
                        text = "Tap chart for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
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
    val palette = lifeAreaChartPalette()

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

            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = overallPercent.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LifeAreaLegend(
    segments: List<LifeAreaCompletion>,
    modifier: Modifier = Modifier
) {
    val palette = lifeAreaChartPalette()
    val entries = segments.mapIndexed { index, segment ->
        segment to palette[index % palette.size]
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.chunked(2).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowEntries.forEach { (segment, color) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = color, shape = CircleShape)
                        )
                        Text(
                            text = "${segment.lifeAreaName} ${segment.completionPercent.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (rowEntries.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChartDetailModal(
    data: List<LifeAreaCompletion>,
    onClose: () -> Unit
) {
    val segments = data.filter { it.totalCount > 0 }
    val palette = lifeAreaChartPalette()

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = "Life Area Completion",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            if (segments.isEmpty()) {
                Text("No assigned life-area habits.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(segments.size) { index ->
                        val segment = segments[index]
                        val color = palette[index % palette.size]
                        val totalCount = segments.sumOf { it.totalCount }
                        val completedCount = segments.sumOf { it.completedCount }
                        val overallPercent = if (totalCount == 0) 0 else (completedCount * 100 / totalCount)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(color = color, shape = CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = segment.lifeAreaName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${segment.completedCount} / ${segment.totalCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${segment.completionPercent.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (segment.completionPercent >= 100f) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    
                    // Summary footer
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Overall Balance",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "${segments.sumOf { it.completedCount }} / ${segments.sumOf { it.totalCount }} completed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PerfectBalanceAnimation(modifier: Modifier = Modifier) {
    var isAnimating by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isAnimating = true
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Subtle pulsing glow effect for balance achievement
        if (isAnimating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            )
        }
        
        // Center checkmark badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


private fun lifeAreaChartPalette(): List<Color> = listOf(
    Color(0xFF0072B2),
    Color(0xFFE69F00),
    Color(0xFF009E73),
    Color(0xFF56B4E9),
    Color(0xFFD55E00),
    Color(0xFFCC79A7),
    Color(0xFF1A73B8),
    Color(0xFF4E7A5D)
)

private fun isCompleted(habit: DailyHabitItem): Boolean {
    return when (habit.type) {
        HabitType.BOOLEAN -> habit.entryBooleanValue == true
        HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> habit.entryNumericValue != null
        HabitType.TIME -> habit.entryNumericValue != null
        HabitType.TEXT -> !habit.entryTextValue.isNullOrBlank()
        HabitType.ROUTINE -> habit.entryBooleanValue == true
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
