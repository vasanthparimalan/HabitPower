package com.example.habitpower.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.MainActivity
import com.example.habitpower.R
import com.example.habitpower.data.HabitPowerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HABIT_REMINDER) return

        HabitReminderScheduler.createReminderChannel(context)

        val habitId = intent.getLongExtra(HabitReminderScheduler.EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return

        val habitName = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_NAME).orEmpty()
        val goalIdentity = intent.getStringExtra(HabitReminderScheduler.EXTRA_GOAL_IDENTITY).orEmpty()
        val goalDescription = intent.getStringExtra(HabitReminderScheduler.EXTRA_GOAL_DESCRIPTION).orEmpty()
        val commitmentTime = intent.getStringExtra(HabitReminderScheduler.EXTRA_COMMITMENT_TIME)
        val commitmentLocation = intent.getStringExtra(HabitReminderScheduler.EXTRA_COMMITMENT_LOCATION).orEmpty()
        val preReminderMinutes = intent.getIntExtra(HabitReminderScheduler.EXTRA_PRE_REMINDER_MINUTES, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val prefs = (appCtx as HabitPowerApp).container.userPreferencesRepository
                val paused = PauseGuardian.isActive(prefs)

                if (!paused) {
                    val enabledChannels = prefs.enabledNotificationChannels.first()
                    if (NotificationChannelType.HABIT_REMINDERS in enabledChannels) {
                        val openIntent = Intent(appCtx, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val openPendingIntent = android.app.PendingIntent.getActivity(
                            appCtx,
                            habitId.toInt(),
                            openIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        val quickCompleteIntent = Intent(appCtx, QuickCompleteReceiver::class.java).apply {
                            action = QuickCompleteReceiver.ACTION_QUICK_COMPLETE
                            putExtra(QuickCompleteReceiver.EXTRA_HABIT_ID, habitId)
                        }
                        val quickCompletePendingIntent = android.app.PendingIntent.getBroadcast(
                            appCtx,
                            (habitId * 2 + 1).toInt(),
                            quickCompleteIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        val title = "Time for $habitName"
                        val body = buildString {
                            append("Be the person who ")
                            append(goalIdentity.ifBlank { "keeps commitments" })
                            append(".")
                            if (goalDescription.isNotBlank()) {
                                append(" ")
                                append(goalDescription)
                            }
                            if (!commitmentTime.isNullOrBlank() || commitmentLocation.isNotBlank()) {
                                append("\n")
                                val whenText = commitmentTime?.let { "At $it" }.orEmpty()
                                val whereText = if (commitmentLocation.isNotBlank()) " in $commitmentLocation" else ""
                                append(whenText)
                                append(whereText)
                            }
                            if (preReminderMinutes in 1..1440) {
                                append("\n")
                                append(String.format(Locale.US, "Reminder set %d min before.", preReminderMinutes))
                            }
                        }

                        val notification = NotificationCompat.Builder(appCtx, HabitReminderScheduler.CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_habit_power)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setAutoCancel(true)
                            .setContentIntent(openPendingIntent)
                            .addAction(R.drawable.ic_habit_power, "Done ✓", quickCompletePendingIntent)
                            .build()

                        if (ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
                        ) {
                            NotificationManagerCompat.from(appCtx).notify(habitId.toInt(), notification)
                        }
                    }
                }

                // Always reschedule so alarms are ready to fire when step-back ends
                val habit = HabitPowerDatabase.getDatabase(appCtx)
                    .habitTrackingDao()
                    .getHabitById(habitId)
                if (habit != null) {
                    HabitReminderScheduler.scheduleForHabit(appCtx, habit)
                } else {
                    HabitReminderScheduler.cancelForHabit(appCtx, habitId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.example.habitpower.ACTION_HABIT_REMINDER"
    }
}
