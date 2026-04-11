package com.example.habitpower

import android.app.Application
import com.example.habitpower.data.AppContainer
import com.example.habitpower.data.DefaultAppContainer

class HabitPowerApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
