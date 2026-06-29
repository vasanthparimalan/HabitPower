package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
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
class HabitTrackingDaoTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val dao get() = db.database.habitTrackingDao()
    private val userDao get() = db.database.userDao()

    private suspend fun insertUser(name: String = "User"): Long =
        userDao.insertUser(UserProfile(name = name))

    private suspend fun insertHabit(name: String = "Habit"): Long =
        dao.insertHabit(HabitDefinition(name = name, goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN))

    @Test
    fun insertHabit_retrieveById() = runBlocking {
        val id = insertHabit("Morning Run")
        val habit = dao.getHabitById(id)
        assertNotNull(habit)
        assertEquals("Morning Run", habit!!.name)
    }

    @Test
    fun upsertAssignment_habitAppearsInUserList() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit("Meditate")
        dao.upsertAssignment(UserHabitAssignment(userId = userId, habitId = habitId))

        val habits = dao.getAssignedHabitsForUser(userId).firstBlocking()
        assertEquals(1, habits.size)
        assertEquals("Meditate", habits.first().name)
    }

    @Test
    fun removeAssignment_habitDisappearsFromUserList() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit("Journaling")
        dao.upsertAssignment(UserHabitAssignment(userId = userId, habitId = habitId))
        dao.clearAssignmentsForUser(userId)

        val habits = dao.getAssignedHabitsForUser(userId).firstBlocking()
        assertTrue(habits.isEmpty())
    }

    @Test
    fun upsertDailyEntry_then_replaceOnSecondUpsert() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit()
        val date = LocalDate.of(2026, 6, 1)

        dao.upsertDailyEntry(DailyHabitEntry(userId, habitId, date, booleanValue = true))
        dao.upsertDailyEntry(DailyHabitEntry(userId, habitId, date, booleanValue = false))

        val entries = dao.getEntriesForHabitInRange(userId, habitId, date, date)
        assertEquals(1, entries.size)
        assertEquals(false, entries.first().booleanValue) // second upsert wins
    }

    @Test
    fun deleteDailyEntry_entryGone() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit()
        val date = LocalDate.of(2026, 6, 5)
        dao.upsertDailyEntry(DailyHabitEntry(userId, habitId, date, booleanValue = true))
        dao.deleteDailyEntry(userId, habitId, date)

        val entries = dao.getEntriesForHabitInRange(userId, habitId, date, date)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun insertAllEntries_bulkInsert500() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit()
        val start = LocalDate.of(2025, 1, 1)
        val entries = (0L until 500L).map { offset ->
            DailyHabitEntry(userId, habitId, start.plusDays(offset), booleanValue = true)
        }
        dao.insertAllEntries(entries)

        val allEntries = dao.getAllEntriesForUser(userId)
        assertEquals(500, allEntries.size)
    }

    @Test
    fun getEntriesForUserInRange_boundaryDatesIncluded() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit()
        val jan1 = LocalDate.of(2026, 1, 1)
        val jan31 = LocalDate.of(2026, 1, 31)
        val feb1 = LocalDate.of(2026, 2, 1)

        dao.upsertDailyEntry(DailyHabitEntry(userId, habitId, jan1, booleanValue = true))
        dao.upsertDailyEntry(DailyHabitEntry(userId, habitId, jan31, booleanValue = true))
        dao.upsertDailyEntry(DailyHabitEntry(userId, habitId, feb1, booleanValue = true))

        val entries = dao.getEntriesForUserInRange(userId, jan1, jan31).firstBlocking()
        assertEquals(2, entries.size) // Jan 1 and Jan 31 in, Feb 1 out
    }

    @Test
    fun getEntriesForUserInRange_excludesOtherUser() = runBlocking {
        val user1 = insertUser("User1")
        val user2 = insertUser("User2")
        val habitId = insertHabit()
        val date = LocalDate.of(2026, 6, 1)

        dao.upsertDailyEntry(DailyHabitEntry(user1, habitId, date, booleanValue = true))
        dao.upsertDailyEntry(DailyHabitEntry(user2, habitId, date, booleanValue = true))

        val entries = dao.getEntriesForUserInRange(user1, date, date).firstBlocking()
        assertEquals(1, entries.size)
        assertEquals(user1, entries.first().userId)
    }

    @Test
    fun getDailyHabitItems_returnsNullEntryForUncompletedHabit() = runBlocking {
        val userId = insertUser()
        val habitId = insertHabit("Cold Shower")
        dao.upsertAssignment(UserHabitAssignment(userId = userId, habitId = habitId))

        val today = LocalDate.now()
        val items = dao.getDailyHabitItems(userId, today).firstBlocking()

        assertEquals(1, items.size)
        // No entry recorded — entry fields should all be null
        assertNull(items.first().entryBooleanValue)
        assertNull(items.first().entryNumericValue)
        assertNull(items.first().entryTextValue)
    }
}
