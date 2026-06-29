package com.example.habitpower.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.data.HabitPowerDatabase
import com.example.habitpower.data.model.HabitType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuickCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_QUICK_COMPLETE) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return

        NotificationManagerCompat.from(context).cancel(habitId.toInt())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val habit = HabitPowerDatabase.getDatabase(appContext)
                    .habitTrackingDao()
                    .getHabitById(habitId) ?: return@launch
                val app = appContext as HabitPowerApp
                val userId = app.container.userPreferencesRepository.activeUserId.first()
                    ?: return@launch
                val today = LocalDate.now()
                val isNumeric = habit.type in listOf(
                    HabitType.NUMBER, HabitType.COUNT, HabitType.DURATION,
                    HabitType.TIMER, HabitType.POMODORO, HabitType.TIME
                )
                if (habit.type == HabitType.TEXT) return@launch
                app.container.habitPowerRepository.saveDailyHabitEntry(
                    userId = userId,
                    date = today,
                    habitId = habitId,
                    type = habit.type,
                    booleanValue = if (!isNumeric) true else null,
                    numericValue = if (isNumeric) habit.targetValue ?: 1.0 else null
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_QUICK_COMPLETE = "com.example.habitpower.ACTION_QUICK_COMPLETE"
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }
}
