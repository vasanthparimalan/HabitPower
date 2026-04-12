package com.example.habitpower.data

import android.content.Context
import com.example.habitpower.gamification.GamificationRepository

interface AppContainer {
    val habitPowerRepository: HabitPowerRepository
    val lifeAreaRepository: com.example.habitpower.data.repository.LifeAreaRepository
    val userPreferencesRepository: UserPreferencesRepository
    val gamificationRepository: GamificationRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database by lazy { HabitPowerDatabase.getDatabase(context) }

    override val habitPowerRepository: HabitPowerRepository by lazy {
        HabitPowerRepository(
            context,
            database.exerciseDao(),
            database.routineDao(),
            database.workoutSessionDao(),
            database.dailyHealthStatDao(),
            database.userDao(),
            database.habitTrackingDao(),
            database.lifeAreaDao(),
            database.quoteDao(),
            UserPreferencesRepository(context)
        )
    }

    override val lifeAreaRepository: com.example.habitpower.data.repository.LifeAreaRepository by lazy {
        com.example.habitpower.data.repository.LifeAreaRepository(database.lifeAreaDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val gamificationRepository: GamificationRepository by lazy {
        GamificationRepository(
            database.userStatsDao(),
            database.habitTrackingDao(),
            database.lifeAreaDao()
        )
    }
}
