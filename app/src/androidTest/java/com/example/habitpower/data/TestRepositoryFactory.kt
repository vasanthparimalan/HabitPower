package com.example.habitpower.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider

/**
 * Builds a HabitPowerRepository wired to the given in-memory test database.
 * Uses a real application context for DataStore and AlarmManager (both safe in
 * instrumented tests — AlarmManager is a no-op on the test device when the app
 * has no background service registered).
 */
fun buildTestRepository(database: HabitPowerDatabase): HabitPowerRepository {
    val context: Context = ApplicationProvider.getApplicationContext()
    return HabitPowerRepository(
        context = context,
        exerciseDao = database.exerciseDao(),
        routineDao = database.routineDao(),
        workoutSessionDao = database.workoutSessionDao(),
        dailyHealthStatDao = database.dailyHealthStatDao(),
        userDao = database.userDao(),
        habitTrackingDao = database.habitTrackingDao(),
        lifeAreaDao = database.lifeAreaDao(),
        quoteDao = database.quoteDao(),
        userPreferencesRepository = UserPreferencesRepository(context),
        routineNotificationSettingsDao = database.routineNotificationSettingsDao(),
        pomodoroSessionDao = database.pomodoroSessionDao(),
        chantDao = database.chantDao(),
        meditationDao = database.meditationDao(),
        taskDao = database.taskDao(),
        database = database
    )
}
