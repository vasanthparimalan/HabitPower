package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DailyEntryTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    private suspend fun newUser(): Long =
        db.database.userDao().insertUser(UserProfile(name = "User"))

    private suspend fun newHabit(type: HabitType = HabitType.BOOLEAN, routineId: Long? = null): Long =
        db.database.habitTrackingDao().insertHabit(
            HabitDefinition(
                name = "Habit",
                goalIdentityStatement = "",
                description = "",
                type = type,
                routineId = routineId,
                recurrenceType = HabitRecurrenceType.DAILY
            )
        )

    @Test
    fun toggleBoolean_noEntry_creates() = runBlocking {
        val userId = newUser()
        val habitId = newHabit()
        val today = LocalDate.now()

        repo.toggleBooleanHabit(userId, habitId, today)

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(1, entries.size)
        assertEquals(true, entries.first().booleanValue)
    }

    @Test
    fun toggleBoolean_existingTrue_deletes() = runBlocking {
        val userId = newUser()
        val habitId = newHabit()
        val today = LocalDate.now()

        repo.toggleBooleanHabit(userId, habitId, today) // create
        repo.toggleBooleanHabit(userId, habitId, today) // delete

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun saveNumericEntry_stored() = runBlocking {
        val userId = newUser()
        val habitId = newHabit(HabitType.NUMBER)
        val today = LocalDate.now()

        repo.saveDailyHabitEntry(userId, today, habitId, HabitType.NUMBER, numericValue = 42.5)

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(42.5, entries.first().numericValue)
        assertNull(entries.first().booleanValue)
    }

    @Test
    fun saveTextEntry_stored() = runBlocking {
        val userId = newUser()
        val habitId = newHabit(HabitType.TEXT)
        val today = LocalDate.now()

        repo.saveDailyHabitEntry(userId, today, habitId, HabitType.TEXT, textValue = "Felt grateful today")

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals("Felt grateful today", entries.first().textValue)
    }

    @Test
    fun saveRoutineEntry_stored() = runBlocking {
        val userId = newUser()
        val habitId = newHabit(HabitType.ROUTINE)
        val today = LocalDate.now()

        repo.saveDailyHabitEntry(userId, today, habitId, HabitType.ROUTINE, booleanValue = true)

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(true, entries.first().booleanValue)
    }

    @Test
    fun updateQuality_onlyQualityUpdated_otherFieldsUnchanged() = runBlocking {
        val userId = newUser()
        val habitId = newHabit()
        val today = LocalDate.now()

        repo.toggleBooleanHabit(userId, habitId, today)
        repo.updateEntryQuality(userId, habitId, today, quality = 2) // steady

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(2, entries.first().quality)
        assertEquals(true, entries.first().booleanValue) // unchanged
    }

    @Test
    fun saveEntry_wrongDate_doesNotAppearInTodayQuery() = runBlocking {
        val userId = newUser()
        val habitId = newHabit()
        val yesterday = LocalDate.now().minusDays(1)
        val today = LocalDate.now()

        repo.saveDailyHabitEntry(userId, yesterday, habitId, HabitType.BOOLEAN, booleanValue = true)

        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertTrue("Yesterday's entry must not appear in today's range query", entries.isEmpty())
    }

    @Test
    fun completeRoutineLinkedHabits_allLinkedHabitsCompleted() = runBlocking {
        val userId = newUser()
        repo.saveActiveUserId(userId)

        val routineId = db.database.routineDao().insertRoutine(
            Routine(name = "Morning Flow", description = "", type = RoutineType.NORMAL)
        )
        val h1 = newHabit(HabitType.ROUTINE, routineId = routineId)
        val h2 = newHabit(HabitType.ROUTINE, routineId = routineId)
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, h1))
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, h2))

        val today = LocalDate.now()
        repo.completeRoutineLinkedHabits(routineId, today)

        val e1 = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, h1, today, today)
        val e2 = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, h2, today, today)
        assertEquals("Habit 1 must be completed", true, e1.firstOrNull()?.booleanValue)
        assertEquals("Habit 2 must be completed", true, e2.firstOrNull()?.booleanValue)
    }
}
