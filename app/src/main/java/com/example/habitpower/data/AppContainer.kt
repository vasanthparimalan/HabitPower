package com.example.habitpower.data

import android.content.Context

/**
 * Simple service locator for app-scoped dependencies.
 *
 * Implementations should provide repository instances used by ViewModels
 * and other UI components. `DefaultAppContainer` wires concrete instances
 * backed by the Room database and simple local repositories.
 */
interface AppContainer {
    val habitPowerRepository: HabitPowerRepository
    val lifeAreaRepository: com.example.habitpower.data.repository.LifeAreaRepository
    val userPreferencesRepository: UserPreferencesRepository
}

/**
 * Default production container that lazily creates the database and repositories.
 */
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
}
