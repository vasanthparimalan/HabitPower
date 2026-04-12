package com.example.habitpower.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habitpower.MainActivity
import com.example.habitpower.R
import com.example.habitpower.data.HabitPowerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = android.app.PendingIntent.getActivity(
            context,
            habitId.toInt(),
            openIntent,
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

        val notification = NotificationCompat.Builder(context, HabitReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habit_power)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33) {
            NotificationManagerCompat.from(context).notify(habitId.toInt(), notification)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val habit = HabitPowerDatabase.getDatabase(appContext)
                    .habitTrackingDao()
                    .getHabitById(habitId)
                if (habit != null) {
                    HabitReminderScheduler.scheduleForHabit(appContext, habit)
                } else {
                    HabitReminderScheduler.cancelForHabit(appContext, habitId)
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
