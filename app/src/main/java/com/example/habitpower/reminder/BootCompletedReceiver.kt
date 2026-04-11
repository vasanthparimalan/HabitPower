package com.example.habitpower.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habitpower.data.HabitPowerDatabase
import com.example.habitpower.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HabitReminderScheduler.createReminderChannel(context)
                val appContext = context.applicationContext
                val database = HabitPowerDatabase.getDatabase(appContext)
                val userPrefs = UserPreferencesRepository(appContext)

                val activeUsers = database.userDao().getActiveUsers().first()
                val storedActiveUserId = userPrefs.activeUserId.first()
                val resolvedActiveUser = when {
                    activeUsers.isEmpty() -> null
                    storedActiveUserId == null -> activeUsers.first()
                    else -> activeUsers.firstOrNull { it.id == storedActiveUserId } ?: activeUsers.first()
                }

                val allHabits = database.habitTrackingDao().getAllHabits().first()
                val assignedIds = if (resolvedActiveUser == null) {
                    emptySet()
                } else {
                    database.habitTrackingDao().getAssignedHabitIdsForUser(resolvedActiveUser.id).first().toSet()
                }

                allHabits.forEach { habit ->
                    if (habit.id in assignedIds) {
                        HabitReminderScheduler.scheduleForHabit(appContext, habit)
                    } else {
                        HabitReminderScheduler.cancelForHabit(appContext, habit.id)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
