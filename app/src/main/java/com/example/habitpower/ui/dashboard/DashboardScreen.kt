package com.example.habitpower.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.gamification.SadhanaScoreEngine
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.theme.AppSpacing
import com.example.habitpower.ui.theme.SectionHeader
import com.example.habitpower.ui.theme.StatusChip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.habitpower.util.SoundPlayer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val DAILY_CUE_PHRASES = listOf(
    "Honor your commitment. The amount doesn't matter today.",
    "Show up. That's the whole game.",
    "Every day is a deposit into who you're becoming.",
    "A small action today keeps the habit alive tomorrow.",
    "Small and consistent beats large and occasional.",
    "Your commitment is your floor, not your ceiling.",
    "Done imperfectly is infinitely better than not done.",
    "Every rep is a vote for the person you're becoming.",
    "You don't need motivation. You need a commitment small enough to always keep.",
    "Identity first, outcomes second. Who are you becoming today?",
    "One percent better. Every single day.",
    "The best time to build a habit was yesterday. The second best time is right now."
)

private const val HOLD_TO_COMPLETE_MS = 760
private const val DEFAULT_TASK_TIME_TEXT = "06:00"

private val COMPLETION_PHRASES = listOf(
    "Done! That's a vote for your best self. 🗳️",
    "Habit honored. Identity reinforced. ✅",
    "One more rep for the person you're becoming. 💪",
    "Showing up is the whole game. You showed up. 🔥",
    "Small win. Real compound interest. 📈",
    "That's the chain staying unbroken. 🔗",
    "Proof over promises — you delivered. ⚡"
)

private fun completionMessage(habitName: String, done: Int, total: Int): String {
    val base = COMPLETION_PHRASES[(done + habitName.length) % COMPLETION_PHRASES.size]
    return when {
        done == total -> "All $total done today! 🎉 Perfect day."
        done == total - 1 -> "$habitName done! One more to go. Almost there 🏁"
        done == 1 -> "$habitName done! First one is always the hardest. $done/$total 🚀"
        else -> "$habitName done! $done of $total today · $base"
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigate: (String) -> Unit = {}
) {
    val heatmapData by viewModel.heatmapData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val todayHabits by viewModel.todayHabits.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val thisWeekConsistency by viewModel.thisWeekConsistency.collectAsState()
    val bestPersonalRecord by viewModel.bestPersonalRecord.collectAsState()
    val lifeAreaCompletion by viewModel.lifeAreaCompletionForSelectedDate.collectAsState()
    val graduatedHabits by viewModel.graduatedHabits.collectAsState()
    val sadhanaScore by viewModel.sadhanaScore.collectAsState()
    val sadhanaWeeklyScores by viewModel.sadhanaWeeklyScores.collectAsState()
    val anchorHabit by viewModel.anchorHabit.collectAsState()
    val stepBackActive by viewModel.stepBackActive.collectAsState()
    val stepBackReturnEpochDay by viewModel.stepBackReturnEpochDay.collectAsState()
    val brightSpotHabit by viewModel.brightSpotHabit.collectAsState()
    val showSeasonAwareness by viewModel.showSeasonAwareness.collectAsState()
    val milestoneWins by viewModel.milestoneWins.collectAsState()
    val weeklyStandupDone by viewModel.weeklyStandupDoneThisWeek.collectAsState()
    val strugglingHabits by viewModel.strugglingHabits.collectAsState()
    val onHoldHabits by viewModel.onHoldHabits.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val completionSoundPrefs by viewModel.completionSoundPrefs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.completionEvents.collect { feedback ->
            val msg = completionMessage(feedback.habitName, feedback.completedCount, feedback.totalCount)
            // Play sound
            if (completionSoundPrefs.soundEnabled) {
                SoundPlayer.playById(completionSoundPrefs.soundId)
            }
            // Mild haptic vibration
            if (completionSoundPrefs.vibrationEnabled) {
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (context.getSystemService(VibratorManager::class.java))?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Vibrator::class.java)
                    }
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    }
                } catch (_: Exception) {}
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    var pendingReflectionHabit by remember { mutableStateOf<DailyHabitItem?>(null) }
    // On-hold flow: which habit the long-press menu is open for, and which is pending a hold.
    var longPressMenuHabit by remember { mutableStateOf<DailyHabitItem?>(null) }
    var habitPendingHold by remember { mutableStateOf<HabitDefinition?>(null) }
    LaunchedEffect(pendingReflectionHabit?.habitId) {
        if (pendingReflectionHabit != null) {
            kotlinx.coroutines.delay(3000)
            pendingReflectionHabit = null
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
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
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Habit Library") },
                                leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                                onClick = { showMoreMenu = false; onNavigate(Screen.HabitLibrary.route) }
                            )
                            DropdownMenuItem(
                                text = { Text("Self Standup") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                                onClick = { showMoreMenu = false; onNavigate(Screen.SelfStandup.route) }
                            )
                            DropdownMenuItem(
                                text = { Text("Help") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = { showMoreMenu = false; onNavigate(Screen.Help.route) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Admin") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = { showMoreMenu = false; onNavigate(Screen.AdminHome.route) }
                            )
                        }
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
                Text("Log Today")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Step-Back status card (shown first when active) ──────────────
            if (stepBackActive) {
                item {
                    StepBackStatusCard(
                        returnEpochDay = stepBackReturnEpochDay,
                        onReturn = { onNavigate(Screen.StepBack.route) }
                    )
                }
            }

            // ── 1. Daily Cue — thin banner, quote behind tap (idea 4) ─────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.nextQuote() }
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val cuePhrase = remember {
                        DAILY_CUE_PHRASES[LocalDate.now().dayOfYear % DAILY_CUE_PHRASES.size]
                    }
                    Text(
                        text = cuePhrase,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    if (dailyQuote.isNotBlank()) {
                        Text(
                            text = "\u201c$dailyQuote\u201d",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            // ── 1a. Sunday Insight — weekly wrap-up shown only on Sundays ─────
            if (java.time.LocalDate.now().dayOfWeek == java.time.DayOfWeek.SUNDAY && thisWeekConsistency > 0) {
                item {
                    SundayInsightCard(
                        weekConsistency = thisWeekConsistency,
                        anchorHabitName = anchorHabit?.habitName,
                        bestArea = lifeAreaCompletion.maxByOrNull { it.completionPercent }
                            ?.takeIf { it.completionPercent > 0f }
                    )
                }
            }

            // ── 1b. Anchor Habit — keystone practice insight ─────────────────
            if (anchorHabit != null) {
                item {
                    AnchorHabitCard(anchorHabit = anchorHabit!!)
                }
            }

            // ── 1c. Season Awareness — gentle prompt when consistency is low ──
            if (showSeasonAwareness) {
                item {
                    SeasonAwarenessCard()
                }
            }

            // ── 2. Today Tasks ────────────────────────────────────────────────
            item {
                var showBackfillMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box {
                        TextButton(onClick = { showBackfillMenu = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Past Days", style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(
                            expanded = showBackfillMenu,
                            onDismissRequest = { showBackfillMenu = false }
                        ) {
                            listOf(1L to "Yesterday", 2L to "2 days ago", 3L to "3 days ago").forEach { (daysBack, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showBackfillMenu = false
                                        onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now().minusDays(daysBack)))
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                fun sortByTime(list: List<DailyHabitItem>): List<DailyHabitItem> =
                    list.sortedBy { habitScheduledTime(it) }

                val sortedPending = sortByTime(todayHabits.filter { !isCompleted(it) })
                val sortedDone = sortByTime(todayHabits.filter { isCompleted(it) })
                var showHonored by rememberSaveable { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    sortedPending.forEach { habit ->
                        TodayTaskRow(
                            habit = habit,
                            isDone = false,
                            onComplete = {
                                if (habit.type == HabitType.BOOLEAN || habit.type == HabitType.ROUTINE) {
                                    viewModel.toggleHabit(habit.habitId, LocalDate.now())
                                    pendingReflectionHabit = habit
                                } else {
                                    onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now()))
                                }
                            },
                            onOpen = {
                                when (habit.type) {
                                    HabitType.POMODORO, HabitType.TIMER ->
                                        onNavigate(Screen.FocusPomodoro.createRoute(habit.habitId))
                                    HabitType.ROUTINE ->
                                        if (habit.routineId != null) {
                                            onNavigate(Screen.ExecuteRoutine.createRoute(habit.routineId))
                                        } else {
                                            onNavigate(Screen.Routines.route)
                                        }
                                    else ->
                                        onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now()))
                                }
                            },
                            onEditTime = { newTime -> viewModel.updateHabitTime(habit.habitId, newTime) },
                            onLongPress = { longPressMenuHabit = habit }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Honored tasks — collapsed by default (idea 1)
                    if (sortedDone.isNotEmpty()) {
                        TextButton(
                            onClick = { showHonored = !showHonored },
                            modifier = Modifier.padding(vertical = 0.dp)
                        ) {
                            Text(
                                text = if (showHonored) "Hide ${sortedDone.size} honored ▲"
                                       else "${sortedDone.size} honored — tap to see ▼",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (showHonored) {
                            sortedDone.forEach { habit ->
                                TodayTaskRow(
                                    habit = habit,
                                    isDone = true,
                                    onComplete = { viewModel.toggleHabit(habit.habitId, LocalDate.now()) },
                                    onOpen = { onNavigate(Screen.DailyCheckIn.createRoute(activeUser?.id, LocalDate.now())) },
                                    onEditTime = { newTime -> viewModel.updateHabitTime(habit.habitId, newTime) },
                                    onLongPress = { longPressMenuHabit = habit }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                }
            }

            // ── Self Standup — after Today so it never blocks the action zone ──
            item {
                SelfStandupBanner(weeklyDone = weeklyStandupDone, onNavigate = onNavigate)
            }

            // ── Habit Health — struggling habits with recovery actions ─────────
            if (strugglingHabits.isNotEmpty()) {
                item {
                    HabitHealthCard(
                        habits = strugglingHabits,
                        onPause = { viewModel.pauseHabit(it) },
                        onEdit = { habitId -> onNavigate(Screen.AdminHabits.createRoute(habitId)) }
                    )
                }
            }

            // ── On Hold — habits temporarily suspended with a return date ────────
            if (onHoldHabits.isNotEmpty()) {
                item {
                    OnHoldSection(
                        habits = onHoldHabits,
                        onResumeEarly = { habit -> viewModel.resumeFromHold(habit) },
                        onEditHold = { habit -> habitPendingHold = habit }
                    )
                }
            }

            // ── Divider: task zone / analytics zone (idea 5) ─────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // ── 3. Your Progress ──────────────────────────────────────────────
            item {
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
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
                SadhanaScoreCard(score = sadhanaScore, weeklyScores = sadhanaWeeklyScores)
            }

            // ── 3b. Bright Spot — 1 working habit during a rough week ─────────
            brightSpotHabit?.let { spot ->
                item {
                    BrightSpotCard(habitName = spot.name, goalStatement = spot.goalIdentityStatement)
                }
            }

            // ── 3c. Life Balance Map — completion % per life area ─────────────
            if (lifeAreaCompletion.isNotEmpty()) {
                item {
                    LifeBalanceCard(lifeAreaCompletion = lifeAreaCompletion)
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
                    MonthlyHeatmap(yearMonth = YearMonth.now().minusMonths(2), heatmapData = heatmapData)
                    MonthlyHeatmap(yearMonth = YearMonth.now().minusMonths(1), heatmapData = heatmapData)
                    MonthlyHeatmap(yearMonth = YearMonth.now(), heatmapData = heatmapData)
                }
            }
            item {
                WeeklyReviewCard(
                    currentStreak = currentStreak,
                    thisWeekConsistency = thisWeekConsistency,
                    bestPersonalRecord = bestPersonalRecord
                )
            }

            // ── 4. Identity Wall — habits you've internalized ────────────────
            if (graduatedHabits.isNotEmpty()) {
                item {
                    MasteryCard(graduatedHabits = graduatedHabits)
                }
            }

            // ── 5. Milestone Wins — identity-level achievements ───────────────
            if (milestoneWins.isNotEmpty()) {
                item {
                    MilestoneWinsCard(milestones = milestoneWins)
                }
            }

            if (!stepBackActive) {
                item {
                    androidx.compose.material3.TextButton(
                        onClick = { onNavigate(Screen.StepBack.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Need to step back for a while? →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }

        // Micro-reflection overlay — auto-dismisses after 3s
        AnimatedVisibility(
            visible = pendingReflectionHabit != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            pendingReflectionHabit?.let { habit ->
                MicroReflectionCard(
                    habitName = habit.name,
                    onQualitySelected = { quality ->
                        viewModel.saveReflection(habit.habitId, LocalDate.now(), quality)
                        pendingReflectionHabit = null
                    },
                    onDismiss = { pendingReflectionHabit = null }
                )
            }
        }
        // ── Long-press context menu ───────────────────────────────────────────
        longPressMenuHabit?.let { pressed ->
            HabitContextMenuSheet(
                habitName = pressed.name,
                onEdit = {
                    longPressMenuHabit = null
                    onNavigate(Screen.AdminHabits.createRoute(pressed.habitId))
                },
                onPutOnHold = {
                    longPressMenuHabit = null
                    habitPendingHold = null  // reset; sheet keyed by habitId below
                    // Re-open as put-on-hold sheet using the same habitId
                    // We store the DailyHabitItem's id in habitPendingHold via a temporary HabitDefinition stub
                    // (only id is needed — putOnHold fetches fresh from DB)
                    habitPendingHold = com.example.habitpower.data.model.HabitDefinition(
                        id = pressed.habitId,
                        name = pressed.name,
                        description = "",
                        type = pressed.type
                    )
                },
                onDismiss = { longPressMenuHabit = null }
            )
        }

        // ── Put-on-hold sheet ────────────────────────────────────────────────
        habitPendingHold?.let { stub ->
            PutOnHoldSheet(
                habitName = stub.name,
                onConfirm = { untilDate ->
                    viewModel.putOnHold(stub.id, untilDate)
                    habitPendingHold = null
                },
                onDismiss = { habitPendingHold = null }
            )
        }

        } // end Box
    }
}

// ── On Hold section ───────────────────────────────────────────────────────────

@Composable
private fun OnHoldSection(
    habits: List<com.example.habitpower.data.model.HabitDefinition>,
    onResumeEarly: (com.example.habitpower.data.model.HabitDefinition) -> Unit,
    onEditHold: (com.example.habitpower.data.model.HabitDefinition) -> Unit
) {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "On Hold",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        habits.forEach { habit ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val returnLabel = habit.pausedUntil?.let { "Returns ${it.format(fmt)}" }
                            ?: "No end date — resume manually"
                        Text(
                            text = returnLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onResumeEarly(habit) }) {
                        Text("Resume")
                    }
                }
            }
        }
    }
}

// ── Long-press context menu bottom sheet ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitContextMenuSheet(
    habitName: String,
    onEdit: () -> Unit,
    onPutOnHold: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = habitName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Edit habit") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.clickable { onEdit() }
            )
            ListItem(
                headlineContent = { Text("Put on hold") },
                supportingContent = { Text("Pause temporarily — streak stays safe") },
                leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                modifier = Modifier.clickable { onPutOnHold() }
            )
        }
    }
}

// ── Put-on-hold duration picker sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PutOnHoldSheet(
    habitName: String,
    onConfirm: (untilDate: java.time.LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val today = java.time.LocalDate.now()
    // Preset durations — cover the most common real-life scenarios
    val presets = listOf(
        "Tomorrow"  to today.plusDays(1),
        "3 days"    to today.plusDays(3),
        "1 week"    to today.plusDays(7),
        "2 weeks"   to today.plusDays(14),
        "1 month"   to today.plusMonths(1),
        "3 months"  to today.plusMonths(3)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Put on hold: $habitName",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Streak is protected. Habit auto-resumes on the return date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            presets.forEach { (label, date) ->
                OutlinedButton(
                    onClick = { onConfirm(date) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        Text(
                            date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // No end date — manual resume only
            OutlinedButton(
                onClick = { onConfirm(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Until I'm back (no end date)")
            }
        }
    }
}

@Composable
private fun StepBackStatusCard(
    returnEpochDay: Long?,
    onReturn: () -> Unit
) {
    val returnDate = returnEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Your practice is resting.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                if (returnDate != null)
                    "Expected return: ${returnDate.format(formatter)}. All reminders are silenced."
                else
                    "Return whenever you're ready. All reminders are silenced.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
            androidx.compose.material3.TextButton(
                onClick = onReturn,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text("Return to my practice →")
            }
        }
    }
}

@Composable
private fun MicroReflectionCard(
    habitName: String,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$habitName — how was it?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { onQualitySelected(1) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😤", style = MaterialTheme.typography.headlineMedium)
                        Text("Tough", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { onQualitySelected(2) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💪", style = MaterialTheme.typography.headlineMedium)
                        Text("Steady", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { onQualitySelected(3) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌟", style = MaterialTheme.typography.headlineMedium)
                        Text("Great", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Skip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun AnchorHabitCard(anchorHabit: com.example.habitpower.gamification.AnchorHabitEngine.AnchorHabit) {
    val multiplierText = "%.1f×".format(anchorHabit.multiplier)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚓", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${anchorHabit.habitName} — completing this makes you $multiplierText more likely to finish the rest.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SadhanaScoreCard(
    score: SadhanaScoreEngine.Score?,
    weeklyScores: List<Pair<LocalDate, Int>>
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val scoreColor = when {
        score == null -> MaterialTheme.colorScheme.onSurfaceVariant
        score.total >= 90 -> tertiary
        score.total >= 70 -> primary
        score.total >= 40 -> secondary
        else -> errorColor
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Yesterday's Practice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (score == null) {
                Text(
                    text = "No data for yesterday.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${score.total}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "out of 100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScoreComponentChip(label = "Habits", value = score.habitComponent)
                    if (score.hasSleepData) ScoreComponentChip(label = "Sleep", value = score.sleepComponent)
                    if (score.hasFocusData) ScoreComponentChip(label = "Focus", value = score.focusComponent)
                }
            }

            if (weeklyScores.isNotEmpty()) {
                SadhanaSparkline(weeklyScores = weeklyScores, barColor = primary, trackColor = outlineVariant)
            }
        }
    }
}

@Composable
private fun ScoreComponentChip(label: String, value: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$value%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SadhanaSparkline(
    weeklyScores: List<Pair<LocalDate, Int>>,
    barColor: Color,
    trackColor: Color
) {
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = weeklyScores.size
                if (count == 0) return@Canvas
                val gap = 4.dp.toPx()
                val totalGaps = gap * (count - 1)
                val barWidth = (size.width - totalGaps) / count
                val maxH = size.height

                weeklyScores.forEachIndexed { i, (_, score) ->
                    val x = i * (barWidth + gap)
                    val fillH = (score / 100f) * maxH
                    drawRect(color = trackColor, topLeft = Offset(x, 0f), size = Size(barWidth, maxH))
                    if (fillH > 0f) {
                        drawRect(color = barColor, topLeft = Offset(x, maxH - fillH), size = Size(barWidth, fillH))
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weeklyScores.forEach { (date, _) ->
                Text(
                    text = date.format(dayFormatter).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MasteryCard(
    graduatedHabits: List<com.example.habitpower.data.model.HabitDefinition>
) {
    var showHabits by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Identity Wall",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "${graduatedHabits.size} habit${if (graduatedHabits.size == 1) "" else "s"} internalized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f)
                    )
                }
                TextButton(onClick = { showHabits = !showHabits }) {
                    Text(if (showHabits) "Hide" else "Show")
                }
            }
            if (showHabits) {
                graduatedHabits.forEach { habit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (habit.goalIdentityStatement.isNotBlank()) {
                                Text(
                                    habit.goalIdentityStatement,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
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
            SectionHeader(title = "Weekly Review")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    icon = Icons.Default.LocalFireDepartment,
                    value = currentStreak.toString(),
                    label = "Current Run",
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TodayTaskRow(
    habit: DailyHabitItem,
    isDone: Boolean,
    onComplete: () -> Unit,
    onOpen: () -> Unit,
    onEditTime: (String?) -> Unit,
    onLongPress: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val title = habit.name.ifBlank { "Untitled habit" }
    val subtitle = todayTaskSubtitle(habit)
    val displayTimeText = habitScheduledTime(habit).format(DateTimeFormatter.ofPattern("HH:mm"))
    val hasPlace = habit.commitmentLocation.isNotBlank()
    var showEditTimeDialog by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDetail) {
        ModalBottomSheet(
            onDismissRequest = { showDetail = false },
            sheetState = detailSheetState
        ) {
            HabitDetailSheet(habit = habit)
        }
    }

    if (showEditTimeDialog) {
        EditTimeDialog(
            currentTime = displayTimeText,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showEditTimeDialog = true },
                    modifier = Modifier.widthIn(min = 72.dp)
                ) {
                    Text(
                        text = displayTimeText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { showDetail = true },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (!isDone && subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hasPlace) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
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
                            text = "Honored",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (habit.type == HabitType.BOOLEAN) {
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
                    ) { Text(if (habit.type == HabitType.ROUTINE) "Run" else "Open") }
                }
            }
        }
    }
}

@Composable
private fun HabitDetailSheet(habit: DailyHabitItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = habit.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (habit.description.isNotBlank()) {
            HabitDetailField(label = "Description", value = habit.description)
        }

        if (habit.goalIdentityStatement.isNotBlank()) {
            HabitDetailField(label = "Who you are becoming", value = habit.goalIdentityStatement)
        }

        if (habit.targetValue != null) {
            val unit = habit.unit?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
            val targetDisplay = if (habit.targetValue == habit.targetValue.toLong().toDouble()) {
                "${habit.targetValue.toLong()}$unit"
            } else {
                "${habit.targetValue}$unit"
            }
            HabitDetailField(label = "Commitment", value = targetDisplay)
        }

        if (!habit.commitmentTime.isNullOrBlank()) {
            HabitDetailField(label = "Scheduled at", value = habit.commitmentTime)
        }

        if (habit.commitmentLocation.isNotBlank()) {
            HabitDetailField(label = "Where", value = habit.commitmentLocation)
        }
    }
}

@Composable
private fun HabitDetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
        HabitType.COUNT, HabitType.NUMBER, HabitType.DURATION -> {
            if (habit.targetValue != null) {
                val unit = habit.unit?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                "Commitment: ${habit.targetValue}$unit"
            } else {
                description
            }
        }
        else -> description
    }
}

private fun habitScheduledTime(habit: DailyHabitItem): LocalTime {
    val parsed = habit.commitmentTime
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    return parsed ?: LocalTime.parse(DEFAULT_TASK_TIME_TEXT)
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
                label = "Current Run",
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
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)

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

                    drawArc(
                        color = color.copy(alpha = 0.12f),
                        startAngle = start,
                        sweepAngle = segmentSweep,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = Size(maxRadius * 2f, maxRadius * 2f)
                    )
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = segmentSweep,
                        useCenter = true,
                        topLeft = Offset(center.x - petalRadius, center.y - petalRadius),
                        size = Size(petalRadius * 2f, petalRadius * 2f)
                    )

                    // Emoji/initial label at ~80% of maxRadius on the background arc
                    val midAngleRad = ((start + segmentSweep / 2) * PI / 180).toFloat()
                    val labelRadius = maxRadius * 0.80f
                    val lx = center.x + labelRadius * cos(midAngleRad.toDouble()).toFloat()
                    val ly = center.y + labelRadius * sin(midAngleRad.toDouble()).toFloat()
                    val label = segment.emoji ?: segment.lifeAreaName.take(1).uppercase()
                    val measured = textMeasurer.measure(label, style = labelStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(lx - measured.size.width / 2f, ly - measured.size.height / 2f),
                        style = labelStyle
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
private fun ChartDetailModal(
    data: List<LifeAreaCompletion>,
    onClose: () -> Unit
) {
    val segments = data.filter { it.totalCount > 0 }
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    val palette = lifeAreaChartPalette()

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = "Life Area Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (segments.isEmpty()) {
                Text("No assigned life-area habits.")
            } else {
                Column {
                    // Scrollable segment list — Column+verticalScroll avoids LazyColumn height issues inside AlertDialog
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        segments.forEachIndexed { index, segment ->
                            val isExpanded = segment.lifeAreaId in expandedIds
                            val progress = segment.completionPercent.coerceIn(0f, 100f) / 100f
                            val isComplete = segment.completionPercent >= 100f
                            val segmentColor = palette[index % palette.size]

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedIds = if (isExpanded)
                                            expandedIds - segment.lifeAreaId
                                        else
                                            expandedIds + segment.lifeAreaId
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                // Header row: emoji | name+count | % | chevron
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = segment.emoji ?: segment.lifeAreaName.take(1).uppercase(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.width(28.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = segment.lifeAreaName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${segment.completedCount}/${segment.totalCount} done",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${segment.completionPercent.toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isComplete) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer(rotationZ = if (isExpanded) 180f else 0f),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Thin progress bar in segment color
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = segmentColor,
                                    trackColor = segmentColor.copy(alpha = 0.15f)
                                )

                                // Expanded habit list
                                if (isExpanded && segment.habits.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 36.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        segment.habits.forEach { habit ->
                                            val habitDone = isCompleted(habit)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            color = if (habitDone) segmentColor
                                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                            shape = CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = habit.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    textDecoration = if (habitDone) TextDecoration.LineThrough else null,
                                                    color = if (habitDone)
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    else
                                                        MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (index < segments.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Overall balance — pinned below scroll, always visible
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${segments.sumOf { it.completedCount }} / ${segments.sumOf { it.totalCount }} completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
private fun SelfStandupBanner(weeklyDone: Boolean, onNavigate: (String) -> Unit) {
    val containerColor = if (weeklyDone)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = if (weeklyDone)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        onClick = { onNavigate(com.example.habitpower.ui.navigation.Screen.SelfStandup.route) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Self Standup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
                if (weeklyDone) {
                    Text(
                        "Weekly review done ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                }
            }
            Text("→", style = MaterialTheme.typography.titleLarge, color = onContainerColor)
        }
    }
}

@Composable
private fun SundayInsightCard(
    weekConsistency: Int,
    anchorHabitName: String?,
    bestArea: LifeAreaCompletion?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Weekly Wrap-Up",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "This week: $weekConsistency% of habits completed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (anchorHabitName != null) {
                Text(
                    "Anchor: $anchorHabitName — your highest-leverage habit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
            }
            if (bestArea != null) {
                Text(
                    "Strongest area: ${bestArea.emoji.orEmpty()} ${bestArea.lifeAreaName} at ${bestArea.completionPercent.toInt()}%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun MilestoneWinsCard(milestones: List<DashboardViewModel.MilestoneWin>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Who You're Becoming",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            milestones.forEach { win ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = win.emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = win.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonAwarenessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Hard week?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Protect your top 3. Showing up on rough days is what matters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun BrightSpotCard(habitName: String, goalStatement: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Bright spot this week",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
                Text(
                    habitName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (goalStatement.isNotBlank()) {
                    Text(
                        goalStatement,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LifeBalanceCard(lifeAreaCompletion: List<LifeAreaCompletion>) {
    val nonEmpty = lifeAreaCompletion.filter { it.totalCount > 0 }
    if (nonEmpty.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Life Balance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            nonEmpty.forEach { area ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (!area.emoji.isNullOrBlank()) area.emoji else "●",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = area.lifeAreaName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${area.completedCount}/${area.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (area.completionPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = when {
                                area.completionPercent >= 80f -> MaterialTheme.colorScheme.primary
                                area.completionPercent >= 50f -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )
                    }
                }
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

@Composable
private fun HabitHealthCard(
    habits: List<StrugglingHabit>,
    onPause: (HabitDefinition) -> Unit,
    onEdit: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Habit Health",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "${habits.size} habit${if (habits.size == 1) "" else "s"} need${if (habits.size == 1) "s" else ""} attention — under 50% in the last 14 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            habits.forEach { struggling ->
                HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = struggling.habit.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "${struggling.completedCount}/${struggling.scheduledCount} completions (${struggling.completionPercent}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onEdit(struggling.habit.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Make it easier", style = MaterialTheme.typography.labelSmall)
                        }
                        FilledTonalButton(
                            onClick = { onEdit(struggling.habit.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Change time", style = MaterialTheme.typography.labelSmall)
                        }
                        FilledTonalButton(
                            onClick = { onPause(struggling.habit) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Pause it", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
