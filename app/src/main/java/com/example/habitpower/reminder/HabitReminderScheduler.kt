package com.example.habitpower.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.habitpower.data.model.HabitDefinition
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object HabitReminderScheduler {
    const val CHANNEL_ID = "habit_commitment_reminders"
    private const val CHANNEL_NAME = "Habit Commitment Reminders"

    const val EXTRA_HABIT_ID = "extra_habit_id"
    const val EXTRA_HABIT_NAME = "extra_habit_name"
    const val EXTRA_GOAL_IDENTITY = "extra_goal_identity"
    const val EXTRA_GOAL_DESCRIPTION = "extra_goal_description"
    const val EXTRA_COMMITMENT_TIME = "extra_commitment_time"
    const val EXTRA_COMMITMENT_LOCATION = "extra_commitment_location"
    const val EXTRA_PRE_REMINDER_MINUTES = "extra_pre_reminder_minutes"

    fun createReminderChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily reminders for habit commitments"
            setShowBadge(true)
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun openDndAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun hasDndPolicyAccess(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.isNotificationPolicyAccessGranted
    }

    fun syncAll(context: Context, habits: List<HabitDefinition>) {
        habits.forEach { habit ->
            scheduleForHabit(context, habit)
        }
    }

    fun scheduleForHabit(context: Context, habit: HabitDefinition) {
        val reminderMinutes = habit.preReminderMinutes
        val commitmentTime = habit.commitmentTime
        if (habit.id <= 0L || reminderMinutes == null || commitmentTime.isNullOrBlank()) {
            cancelForHabit(context, habit.id)
            return
        }

        val time = runCatching { LocalTime.parse(commitmentTime) }.getOrNull()
        if (time == null) {
            cancelForHabit(context, habit.id)
            return
        }

        val now = LocalDateTime.now()
        val trigger = HabitRecurrenceCalculator.nextReminderTrigger(
            now = now,
            commitmentTime = time,
            reminderMinutes = reminderMinutes,
            habit = habit
        )
        if (trigger == null) {
            cancelForHabit(context, habit.id)
            return
        }

        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = buildPendingIntent(context, habit)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelForHabit(context: Context, habitId: Long) {
        if (habitId <= 0L) return
        val pendingIntent = buildPendingIntent(context, habitId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(context: Context, habit: HabitDefinition): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_NAME, habit.name)
            putExtra(EXTRA_GOAL_IDENTITY, habit.goalIdentityStatement)
            putExtra(EXTRA_GOAL_DESCRIPTION, habit.description)
            putExtra(EXTRA_COMMITMENT_TIME, habit.commitmentTime)
            putExtra(EXTRA_COMMITMENT_LOCATION, habit.commitmentLocation)
            putExtra(EXTRA_PRE_REMINDER_MINUTES, habit.preReminderMinutes ?: 0)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(habit.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildPendingIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(habitId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeFor(habitId: Long): Int = (habitId % Int.MAX_VALUE).toInt()
}
