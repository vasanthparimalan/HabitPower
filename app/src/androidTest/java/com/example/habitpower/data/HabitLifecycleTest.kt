package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HabitLifecycleTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    private suspend fun setup(): Pair<Long, Long> {
        val userId = db.database.userDao().insertUser(UserProfile(name = "User"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Target", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, habitId))
        return userId to habitId
    }

    @Test
    fun activeHabit_appearsInDailyItems() = runBlocking {
        val (userId, _) = setup()
        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertEquals(1, items.size)
    }

    @Test
    fun pausedHabit_hiddenFromDailyItems() = runBlocking {
        val (userId, habitId) = setup()
        val habit = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(habit, HabitLifecycleStatus.PAUSED)

        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun pausedHabit_reactivated_reappearsInDailyItems() = runBlocking {
        val (userId, habitId) = setup()
        val habit = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(habit, HabitLifecycleStatus.PAUSED)
        val paused = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(paused, HabitLifecycleStatus.ACTIVE)

        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertEquals(1, items.size)
    }

    @Test
    fun retiredHabit_hiddenFromAllTracking() = runBlocking {
        val (userId, habitId) = setup()
        val habit = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(habit, HabitLifecycleStatus.RETIRED)

        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun graduatedHabit_inGraduatedList_notInDaily() = runBlocking {
        val (userId, habitId) = setup()
        val habit = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(habit, HabitLifecycleStatus.GRADUATED)

        val graduated = repo.getGraduatedHabitsForUser(userId).first()
        val daily = repo.getDailyHabitItems(userId, LocalDate.now()).first()

        assertEquals(1, graduated.size)
        assertTrue(daily.isEmpty())
    }

    @Test
    fun isActiveFalse_hiddenRegardlessOfLifecycle() = runBlocking {
        val (userId, habitId) = setup()
        val habit = db.database.habitTrackingDao().getHabitById(habitId)!!
        db.database.habitTrackingDao().updateHabit(habit.copy(isActive = false))

        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertTrue(items.isEmpty())
    }
}
