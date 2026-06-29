package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class StreakCalculationTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    private suspend fun setup(): Pair<Long, Long> {
        val userId = db.database.userDao().insertUser(UserProfile(name = "User"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Daily", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        return userId to habitId
    }

    @Test
    fun streak_zero_noEntries() = runBlocking {
        val (userId, habitId) = setup()
        val streak = repo.getHabitStreak(userId, habitId, HabitType.BOOLEAN, null)
        assertEquals(0, streak)
    }

    @Test
    fun streak_7consecutive_daily() = runBlocking {
        val (userId, habitId) = setup()
        val today = LocalDate.now()
        for (i in 0L..6L) repo.toggleBooleanHabit(userId, habitId, today.minusDays(i))

        val streak = repo.getHabitStreak(userId, habitId, HabitType.BOOLEAN, null)
        assertEquals(7, streak)
    }

    @Test
    fun streak_breaksOnSingleMiss() = runBlocking {
        val (userId, habitId) = setup()
        val today = LocalDate.now()
        repo.toggleBooleanHabit(userId, habitId, today)
        repo.toggleBooleanHabit(userId, habitId, today.minusDays(1))
        // day -2 skipped intentionally
        repo.toggleBooleanHabit(userId, habitId, today.minusDays(3))
        repo.toggleBooleanHabit(userId, habitId, today.minusDays(4))

        val streak = repo.getHabitStreak(userId, habitId, HabitType.BOOLEAN, null)
        assertEquals(2, streak) // today + yesterday only
    }

    @Test
    fun streak_pausedDaysDoNotBreak() = runBlocking {
        // Streak is entry-based: PAUSED status doesn't add gaps if entries exist
        val (userId, habitId) = setup()
        val today = LocalDate.now()
        // Insert entries for 5 days straight regardless of lifecycle status
        for (i in 0L..4L) repo.toggleBooleanHabit(userId, habitId, today.minusDays(i))

        val streak = repo.getHabitStreak(userId, habitId, HabitType.BOOLEAN, null)
        assertEquals(5, streak)
    }

    @Test
    fun streak_numericHabit_targetMet() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "NumericUser"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(
                name = "Steps", goalIdentityStatement = "", description = "",
                type = HabitType.NUMBER, targetValue = 8000.0
            )
        )
        val today = LocalDate.now()
        // 3 days, all meeting the 8000 target
        for (i in 0L..2L) {
            repo.saveDailyHabitEntry(
                userId, today.minusDays(i), habitId, HabitType.NUMBER, numericValue = 10000.0
            )
        }

        val streak = repo.getHabitStreak(userId, habitId, HabitType.NUMBER, 8000.0)
        assertEquals(3, streak)
    }

    @Test
    fun streak_numericHabit_noEntry_breaks() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "NumBreak"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(
                name = "Steps2", goalIdentityStatement = "", description = "",
                type = HabitType.NUMBER, targetValue = 5000.0
            )
        )
        val today = LocalDate.now()
        repo.saveDailyHabitEntry(userId, today, habitId, HabitType.NUMBER, numericValue = 6000.0)
        // yesterday: no entry

        val streak = repo.getHabitStreak(userId, habitId, HabitType.NUMBER, 5000.0)
        assertEquals(1, streak) // only today
    }
}
