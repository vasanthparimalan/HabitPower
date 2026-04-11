package com.example.habitpower.ui.widget

import android.content.Context
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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.MainActivity
import com.example.habitpower.R
import com.example.habitpower.data.WidgetHabit
import com.example.habitpower.data.WidgetState
import com.example.habitpower.ui.navigation.Screen
import kotlinx.coroutines.flow.firstOrNull

val navigationScreenKey = ActionParameters.Key<String>("screen")

class HabitPowerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
        val widgetState = repository.getWidgetState().firstOrNull() ?: WidgetState()

        provideContent {
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
                .background(androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF141414), night = androidx.compose.ui.graphics.Color(0xFF141414)))
                .padding(12.dp)
        ) {
            if (state.userName.isBlank()) {
                Text(
                    text = "No users configured",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White))
                )
                return@Column
            }

            Header(state.userName)
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (state.habits.isEmpty()) {
                Text(
                    text = "No routines found today",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.Gray, night = androidx.compose.ui.graphics.Color.Gray))
                )
            } else {
                state.habits.take(3).forEachIndexed { index, habit ->
                    val bgRes = if (index == 0) R.drawable.widget_habit_bg_identity else R.drawable.widget_habit_bg
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(bgRes))
                            .padding(8.dp)
                    ) {
                        WidgetHabitItemView(
                            habit = habit,
                            isIdentityPin = index == 0
                        )
                    }
                    if (index < state.habits.take(3).lastIndex) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetHabitItemView(
        habit: WidgetHabit,
        isIdentityPin: Boolean
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getStreakDisplay(habit.name, habit.streak, 1, isIdentityPin),
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White),
                        fontWeight = if (isIdentityPin) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isIdentityPin) 14.sp else 12.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Text(
                    text = if (habit.isCompleted) " ✅ " else " ❌ ",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White), fontWeight = FontWeight.Bold)
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
                text = "$userName's Dashboard",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "Refresh Widget",
                modifier = GlanceModifier.size(36.dp).padding(8.dp).clickable(
                    actionRunCallback<RefreshWidgetCallback>()
                )
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_checkin),
                contentDescription = "Open App",
                modifier = GlanceModifier.size(36.dp).padding(8.dp).clickable(
                    actionStartActivity<MainActivity>(actionParametersOf(navigationScreenKey to Screen.DailyCheckIn.baseRoute))
                )
            )
        }
    }

    private fun getStreakDisplay(name: String, streak: Int, baseDays: Int, isIdentityPin: Boolean): String {
        val x = baseDays.coerceAtLeast(1)
        val emoji = when {
            streak == 0 -> "🌑"
            streak < x -> "🔥🙂"
            streak < 2 * x -> "🔥🔥😄"
            streak < 3 * x -> "🔥🔥🔥💪"
            else -> "🌟🔥🔥🔥👑"
        }
        val prefix = if (isIdentityPin) {
            "⭐️"
        } else {
            when {
                name.contains("Sleep", ignoreCase = true) -> "🛌"
                name.contains("Step", ignoreCase = true) -> "🚶"
                name.contains("Medit", ignoreCase = true) -> "🧘"
                name.contains("Routine", ignoreCase = true) || name.contains("Workout", ignoreCase = true) -> "💪"
                else -> "📌"
            }
        }
        return "$prefix $name $emoji $streak"
    }
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
        repo.updateWidgetState()
        HabitPowerWidget().update(context, glanceId)
    }
}
