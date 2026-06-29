package com.example.habitpower.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Schedules the three daily gamification alarms:
 *  1. Morning brief  — 07:30 every day
 *  2. Mid-day nudge  — 12:00 every day (only fires notification if 0 habits done)
 *  3. Evening check  — 20:30 every day (only fires notification if day incomplete)
 *
 * Uses the same AlarmManager pattern as [HabitReminderScheduler] to stay
 * consistent and avoid adding a new dependency.
 */
object GamificationScheduler {

    const val CHANNEL_ID_DAILY_BRIEF    = "gamification_daily_brief"
    const val CHANNEL_ID_NUDGE          = "gamification_nudge"
    const val CHANNEL_ID_CELEBRATION    = "gamification_celebration"
    const val CHANNEL_ID_WEEKLY_INSIGHT = "gamification_weekly_insight"

    private const val RC_MORNING   = 90_001
    private const val RC_MIDDAY    = 90_002
    private const val RC_EVENING   = 90_003
    private const val RC_WEEKLY    = 90_004

    private val TIME_MORNING = LocalTime.of(7, 30)
    private val TIME_MIDDAY  = LocalTime.of(12, 0)
    private val TIME_EVENING = LocalTime.of(20, 30)
    private val TIME_WEEKLY  = LocalTime.of(19, 0)

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fun make(id: String, name: String, importance: Int, desc: String) {
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(NotificationChannel(id, name, importance).apply {
                    description = desc
                    setShowBadge(true)
                })
            }
        }
        make(CHANNEL_ID_DAILY_BRIEF, "Daily Habit Brief", NotificationManager.IMPORTANCE_DEFAULT,
            "Morning summary of yesterday's habits and today's focus areas")
        make(CHANNEL_ID_NUDGE, "Habit Nudge", NotificationManager.IMPORTANCE_LOW,
            "Gentle mid-day reminders when no habits have been checked yet")
        make(CHANNEL_ID_CELEBRATION, "Habit Celebration", NotificationManager.IMPORTANCE_HIGH,
            "Celebration notifications when you complete all your habits")
        make(CHANNEL_ID_WEEKLY_INSIGHT, "Weekly Insight", NotificationManager.IMPORTANCE_DEFAULT,
            "Sunday evening summary of your week's habit patterns")
    }

    fun scheduleAll(context: Context) {
        createChannels(context)
        scheduleAlarm(context, MorningBriefReceiver::class.java, RC_MORNING, TIME_MORNING)
        scheduleAlarm(context, MidDayNudgeReceiver::class.java,  RC_MIDDAY,  TIME_MIDDAY)
        scheduleAlarm(context, EveningCheckReceiver::class.java, RC_EVENING, TIME_EVENING)
        scheduleWeeklyAlarm(context)
    }

    fun cancelAll(context: Context) {
        cancelAlarm(context, MorningBriefReceiver::class.java, RC_MORNING)
        cancelAlarm(context, MidDayNudgeReceiver::class.java,  RC_MIDDAY)
        cancelAlarm(context, EveningCheckReceiver::class.java, RC_EVENING)
        cancelAlarm(context, WeeklyInsightReceiver::class.java, RC_WEEKLY)
    }

    fun scheduleWeeklyAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeeklyInsightReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, RC_WEEKLY, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = LocalDateTime.now()
        var trigger = LocalDate.now()
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .atTime(TIME_WEEKLY)
        if (!trigger.isAfter(now)) trigger = trigger.plusWeeks(1)

        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canScheduleExact = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()
        if (canScheduleExact) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, pi), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    private fun scheduleAlarm(context: Context, receiverClass: Class<*>, requestCode: Int, time: LocalTime) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = LocalDateTime.now()
        var trigger = LocalDateTime.of(LocalDate.now(), time)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)

        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val canScheduleExact = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()
        if (canScheduleExact) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, pi), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    private fun cancelAlarm(context: Context, receiverClass: Class<*>, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, receiverClass),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        am.cancel(pi)
    }
}
