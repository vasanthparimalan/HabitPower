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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepBackReturnReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_STEP_BACK_RETURN) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val prefs = (appCtx as HabitPowerApp).container.userPreferencesRepository
                prefs.setStepBack(false, null)

                if (ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
                ) return@launch

                HabitReminderScheduler.createReminderChannel(appCtx)
                val openIntent = Intent(appCtx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = android.app.PendingIntent.getActivity(
                    appCtx, NOTIF_ID, openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(appCtx, HabitReminderScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_habit_power)
                    .setContentTitle("You set aside some time.")
                    .setContentText("Ready to return whenever you are.")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Your practice has been resting. Open the app whenever you're ready — no pressure, no catch-up required."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .build()
                NotificationManagerCompat.from(appCtx).notify(NOTIF_ID, notification)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_STEP_BACK_RETURN = "com.example.habitpower.ACTION_STEP_BACK_RETURN"
        const val NOTIF_ID = 9_010
        private const val REQUEST_CODE = 9_010

        fun schedule(context: Context, returnEpochDay: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, StepBackReturnReceiver::class.java).apply {
                action = ACTION_STEP_BACK_RETURN
            }
            val pi = android.app.PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val triggerMillis = java.time.LocalDate.ofEpochDay(returnEpochDay)
                .atTime(9, 0)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val pi = android.app.PendingIntent.getBroadcast(
                context, REQUEST_CODE,
                Intent(context, StepBackReturnReceiver::class.java).apply {
                    action = ACTION_STEP_BACK_RETURN
                },
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
            ) ?: return
            am.cancel(pi)
        }
    }
}
