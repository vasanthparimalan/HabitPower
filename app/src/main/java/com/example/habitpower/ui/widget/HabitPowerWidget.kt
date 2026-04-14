package com.example.habitpower.ui.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.MainActivity
import com.example.habitpower.R
import com.example.habitpower.data.WidgetHabit
import com.example.habitpower.data.WidgetState
import com.example.habitpower.ui.navigation.Screen

val navigationScreenKey = ActionParameters.Key<String>("screen")

private val White = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color.White,
    night = androidx.compose.ui.graphics.Color.White
)
private val Gray = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF9E9E9E),
    night = androidx.compose.ui.graphics.Color(0xFF9E9E9E)
)
private val Green = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF4CAF50),
    night = androidx.compose.ui.graphics.Color(0xFF4CAF50)
)
private val DarkBg = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF141414),
    night = androidx.compose.ui.graphics.Color(0xFF141414)
)
private val ProgressTrack = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF2C2C2C),
    night = androidx.compose.ui.graphics.Color(0xFF2C2C2C)
)
private val ProgressFill = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF66BB6A),
    night = androidx.compose.ui.graphics.Color(0xFF66BB6A)
)

class HabitPowerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
        try { repository.updateWidgetState() } catch (_: Exception) {}
        val widgetStateFlow = repository.getWidgetState()

        provideContent {
            val widgetState by widgetStateFlow.collectAsState(initial = WidgetState())
            GlanceTheme {
                WidgetContentLayout(state = widgetState)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContentLayout(state: WidgetState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(12.dp)
        ) {
            if (state.userName.isBlank()) {
                Text(
                    text = "No users configured",
                    style = TextStyle(color = Gray)
                )
                return@Column
            }

            Header(state.userName)
            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress bar + count
            if (state.totalCount > 0) {
                ProgressSection(
                    completedCount = state.completedCount,
                    totalCount = state.totalCount
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            when {
                state.totalCount == 0 -> {
                    Text(
                        text = "No habits scheduled for today",
                        style = TextStyle(color = Gray, fontSize = 12.sp)
                    )
                }
                state.habits.isEmpty() -> {
                    // All habits are done
                    AllDoneView()
                }
                else -> {
                    // Pending habits list
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        itemsIndexed(state.habits) { index, habit ->
                            val bgRes = if (index == 0) R.drawable.widget_habit_bg_identity
                                        else R.drawable.widget_habit_bg
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .background(ImageProvider(bgRes))
                                    .padding(8.dp)
                                    .clickable(
                                        actionStartActivity<MainActivity>(
                                            actionParametersOf(navigationScreenKey to habit.navigateTo)
                                        )
                                    )
                            ) {
                                HabitRow(habit = habit, isPinned = index == 0)
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ProgressSection(completedCount: Int, totalCount: Int) {
        val ratio = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
        val pct = (ratio * 100).toInt()

        // "5 of 8 done · 62%" label
        Text(
            text = "$completedCount of $totalCount done · $pct%",
            style = TextStyle(
                color = if (pct >= 100) Green else White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))

        // Segmented progress bar using a Row of filled/empty slots
        ProgressBar(ratio = ratio)
    }

    @androidx.compose.runtime.Composable
    private fun ProgressBar(ratio: Float) {
        // Build a simple two-segment bar: filled portion + remaining portion
        // We use a Row with two Box children whose widths are set proportionally.
        // Since Glance doesn't support fractional weight, we approximate using 20 fixed segments.
        val totalSegments = 20
        val filledSegments = (ratio * totalSegments).toInt().coerceIn(0, totalSegments)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(5.dp)
                .background(ProgressTrack)
        ) {
            if (filledSegments > 0) {
                // Each segment is 1/20 of available width — use defaultWeight on each
                repeat(filledSegments) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ProgressFill)
                    ) {}
                }
            }
            if (filledSegments < totalSegments) {
                repeat(totalSegments - filledSegments) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ProgressTrack)
                    ) {}
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AllDoneView() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                style = TextStyle(fontSize = 28.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "All done for today!",
                style = TextStyle(color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Great work. See you tomorrow.",
                style = TextStyle(color = Gray, fontSize = 11.sp)
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun HabitRow(habit: WidgetHabit, isPinned: Boolean) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habitLabel(habit.name, habit.streak, isPinned),
                style = TextStyle(
                    color = White,
                    fontWeight = if (isPinned) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isPinned) 13.sp else 12.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            // Streak badge — show only if streak > 0
            if (habit.streak > 0) {
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "🔥${habit.streak}",
                    style = TextStyle(color = White, fontSize = 11.sp)
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Header(userName: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = userName,
                style = TextStyle(color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "Refresh",
                modifier = GlanceModifier.size(32.dp).padding(6.dp).clickable(
                    actionRunCallback<RefreshWidgetCallback>()
                )
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_checkin),
                contentDescription = "Open App",
                modifier = GlanceModifier.size(32.dp).padding(6.dp).clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(navigationScreenKey to Screen.DailyCheckIn.baseRoute)
                    )
                )
            )
        }
    }

    private fun habitLabel(name: String, streak: Int, isPinned: Boolean): String {
        val prefix = if (isPinned) "⭐ " else when {
            name.contains("Sleep", ignoreCase = true) -> "🛌 "
            name.contains("Step", ignoreCase = true) -> "🚶 "
            name.contains("Medit", ignoreCase = true) -> "🧘 "
            name.contains("Routine", ignoreCase = true) ||
                name.contains("Workout", ignoreCase = true) -> "💪 "
            else -> "• "
        }
        return "$prefix$name"
    }
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repo = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
            repo.updateWidgetState()
            HabitPowerWidget().updateAll(context)
        } catch (_: Exception) {}
    }
}
