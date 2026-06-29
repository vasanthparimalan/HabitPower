package com.example.habitpower

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.habitpower.data.AppContainer
import com.example.habitpower.data.DefaultAppContainer
import com.example.habitpower.data.sync.DriveSyncWorker
import com.example.habitpower.reminder.GamificationScheduler
import com.example.habitpower.reminder.HabitReminderScheduler
import com.example.habitpower.util.CrashLogger
import com.example.habitpower.util.LocalCrashHandler
import com.example.habitpower.util.TtsPlayer
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HabitPowerApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        LocalCrashHandler.checkPreviousCrash(this)
        LocalCrashHandler.install(this)
        container = DefaultAppContainer(this)
        HabitReminderScheduler.createReminderChannel(this)
        GamificationScheduler.scheduleAll(this)
        TtsPlayer.init(this)
        scheduleDriveSync()
        CoroutineScope(Dispatchers.IO).launch {
            container.habitPowerRepository.seedExercisesIfNeeded(
                container.exerciseLibraryRepository.getAll()
            )
            container.habitPowerRepository.seedRoutinesIfNeeded()
            container.habitPowerRepository.seedChantsIfNeeded()
            // Set active user as Crashlytics identifier for crash reports
            container.userPreferencesRepository.activeUserId.firstOrNull()?.let { userId ->
                CrashLogger.setUserId(userId)
            }
        }
    }

    private fun scheduleDriveSync() {
        val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "drive_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onTerminate() {
        TtsPlayer.shutdown()
        super.onTerminate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
