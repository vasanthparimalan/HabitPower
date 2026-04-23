package com.example.habitpower

import android.app.Application
import com.example.habitpower.data.AppContainer
import com.example.habitpower.data.DefaultAppContainer
import com.example.habitpower.reminder.GamificationScheduler
import com.example.habitpower.reminder.HabitReminderScheduler
import com.example.habitpower.util.TtsPlayer
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitPowerApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        HabitReminderScheduler.createReminderChannel(this)
        GamificationScheduler.scheduleAll(this)
        TtsPlayer.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            container.habitPowerRepository.seedExercisesIfNeeded(
                container.exerciseLibraryRepository.getAll()
            )
        }
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
