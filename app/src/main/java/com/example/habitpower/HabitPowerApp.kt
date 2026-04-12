package com.example.habitpower

import android.app.Application
import com.example.habitpower.data.AppContainer
import com.example.habitpower.data.DefaultAppContainer
import com.example.habitpower.reminder.GamificationScheduler
import com.example.habitpower.reminder.HabitReminderScheduler

class HabitPowerApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        // Set up notification channels and schedule daily gamification alarms
        HabitReminderScheduler.createReminderChannel(this)
        GamificationScheduler.scheduleAll(this)
    }
}
