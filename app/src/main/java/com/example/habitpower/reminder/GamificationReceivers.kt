package com.example.habitpower.reminder

import android.Manifest
import android.app.PendingIntent
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
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.gamification.GamificationRepository
import com.example.habitpower.gamification.GamificationEngine
import com.example.habitpower.gamification.MotivationContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at 07:30 every morning.
 * Sends a summary of yesterday's performance + today's habit plan.
 */
class MorningBriefReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        GamificationScheduler.createChannels(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val db = HabitPowerDatabase.getDatabase(appCtx)
                val prefs = UserPreferencesRepository(appCtx)

                val activeUsers = db.userDao().getActiveUsers().first()
                val storedId = prefs.activeUserId.first()
                val user = when {
                    activeUsers.isEmpty() -> return@launch
                    storedId == null -> activeUsers.first()
                    else -> activeUsers.firstOrNull { it.id == storedId } ?: activeUsers.first()
                }

                val repo = GamificationRepository(db.userStatsDao(), db.habitTrackingDao(), db.lifeAreaDao())
                val data = repo.getMorningBriefData(user.id)

                val title = MotivationContent.morningBriefTitle(user.name)
                val body = MotivationContent.morningBriefBody(
                    userName = user.name,
                    yesterdayCompleted = data.yesterdayCompleted,
                    yesterdayTotal = data.yesterdayTotal,
                    streak = data.stats.currentStreak,
                    level = data.stats.level,
                    weakestAreaName = data.weakestAreaName,
                    todayHabitCount = data.todayHabitCount
                )

                sendNotification(appCtx, title, body, NOTIF_ID)

                // Reschedule for tomorrow
                GamificationScheduler.scheduleAll(appCtx)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val NOTIF_ID = 9_001
    }
}

/**
 * Fires at 12:00 every day.
 * Only sends a notification if the user has checked in 0 habits today.
 */
class MidDayNudgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        GamificationScheduler.createChannels(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val db = HabitPowerDatabase.getDatabase(appCtx)
                val prefs = UserPreferencesRepository(appCtx)

                val activeUsers = db.userDao().getActiveUsers().first()
                val storedId = prefs.activeUserId.first()
                val user = when {
                    activeUsers.isEmpty() -> return@launch
                    storedId == null -> activeUsers.first()
                    else -> activeUsers.firstOrNull { it.id == storedId } ?: activeUsers.first()
                }

                val repo = GamificationRepository(db.userStatsDao(), db.habitTrackingDao(), db.lifeAreaDao())
                val data = repo.getEveningData(user.id) // same structure works for midday
                if (data.completedCount > 0) return@launch // already on it — don't nudge

                val title = MotivationContent.midDayNudgeTitle(user.name)
                val body  = MotivationContent.midDayNudgeBody(data.totalCount)
                sendNotification(appCtx, title, body, NOTIF_ID,
                    channelId = GamificationScheduler.CHANNEL_ID_NUDGE,
                    priority = NotificationCompat.PRIORITY_LOW)

                // Reschedule tomorrow
                GamificationScheduler.scheduleAll(appCtx)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val NOTIF_ID = 9_002
    }
}

/**
 * Fires at 20:30 every evening.
 * If the day is already perfect, skips or sends a congratulatory note.
 * Otherwise sends an evening nudge listing pending habits.
 */
class EveningCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        GamificationScheduler.createChannels(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val db = HabitPowerDatabase.getDatabase(appCtx)
                val prefs = UserPreferencesRepository(appCtx)

                val activeUsers = db.userDao().getActiveUsers().first()
                val storedId = prefs.activeUserId.first()
                val user = when {
                    activeUsers.isEmpty() -> return@launch
                    storedId == null -> activeUsers.first()
                    else -> activeUsers.firstOrNull { it.id == storedId } ?: activeUsers.first()
                }

                val repo = GamificationRepository(db.userStatsDao(), db.habitTrackingDao(), db.lifeAreaDao())
                val data = repo.getEveningData(user.id)

                if (data.totalCount == 0) return@launch // no habits assigned, skip

                if (data.completedCount == data.totalCount) {
                    // Already done — send a brief celebration instead
                    val title = MotivationContent.dayCrushedTitle(user.name)
                    val xpGained = GamificationEngine.computeXpGain(
                        data.completedCount, true, data.stats.currentStreak)
                    val body = MotivationContent.dayCrushedBody(data.totalCount, data.stats.currentStreak, xpGained)
                    sendNotification(appCtx, title, body, NOTIF_ID,
                        channelId = GamificationScheduler.CHANNEL_ID_CELEBRATION)
                } else {
                    val title = MotivationContent.eveningNudgeTitle(user.name)
                    val body  = MotivationContent.eveningNudgeBody(
                        data.completedCount, data.totalCount, data.pendingHabitNames)
                    sendNotification(appCtx, title, body, NOTIF_ID)
                }

                // Reschedule for tomorrow
                GamificationScheduler.scheduleAll(appCtx)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val NOTIF_ID = 9_003
    }
}

// ── Shared helper ─────────────────────────────────────────────────────────────
private fun sendNotification(
    context: Context,
    title: String,
    body: String,
    notifId: Int,
    channelId: String = GamificationScheduler.CHANNEL_ID_DAILY_BRIEF,
    priority: Int = NotificationCompat.PRIORITY_DEFAULT
) {
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return

    val openIntent = PendingIntent.getActivity(
        context, notifId,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_habit_power)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(priority)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .build()

    NotificationManagerCompat.from(context).notify(notifId, notification)
}
