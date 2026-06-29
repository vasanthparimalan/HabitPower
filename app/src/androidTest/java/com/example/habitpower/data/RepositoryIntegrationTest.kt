package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    /** Shorthand — `unit` and `targetValue` are required params with no default. */
    private suspend fun habit(
        name: String,
        type: HabitType = HabitType.BOOLEAN,
        targetValue: Double? = null,
        unit: String? = null,
        recurrenceType: HabitRecurrenceType = HabitRecurrenceType.DAILY
    ) = repo.createHabit(
        name = name,
        goalIdentityStatement = "",
        description = "",
        type = type,
        unit = unit,
        targetValue = targetValue,
        recurrenceType = recurrenceType
    )

    // ── User ──────────────────────────────────────────────────────────────────

    @Test
    fun createUser_saveActiveUserId_resolvedUserCorrect() = runBlocking {
        val userId = repo.createUser("Ravi")
        repo.saveActiveUserId(userId)
        val resolved = repo.getResolvedActiveUser().first()
        assertNotNull(resolved)
        assertEquals("Ravi", resolved!!.name)
    }

    // ── Habit + Assignment ────────────────────────────────────────────────────

    @Test
    fun createHabit_assignToUser_appearsInDailyItems() = runBlocking {
        val userId = repo.createUser("Priya")
        val habitId = habit("Journaling")
        repo.replaceAssignmentsForUser(userId, listOf(habitId))
        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertEquals(1, items.size)
        assertEquals("Journaling", items.first().name)
    }

    // ── Boolean toggle ────────────────────────────────────────────────────────

    @Test
    fun toggleBooleanHabit_on_entryCreated() = runBlocking {
        val userId = repo.createUser("Sam")
        val habitId = habit("Cold Shower")
        val today = LocalDate.now()
        repo.toggleBooleanHabit(userId, habitId, today)
        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(1, entries.size)
        assertEquals(true, entries.first().booleanValue)
    }

    @Test
    fun toggleBooleanHabit_off_entryDeleted() = runBlocking {
        val userId = repo.createUser("Sam2")
        val habitId = habit("Workout")
        val today = LocalDate.now()
        repo.toggleBooleanHabit(userId, habitId, today)
        repo.toggleBooleanHabit(userId, habitId, today)
        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertTrue("Second toggle must delete the entry", entries.isEmpty())
    }

    // ── Entry types ───────────────────────────────────────────────────────────

    @Test
    fun saveDailyHabitEntry_numeric_persisted() = runBlocking {
        val userId = repo.createUser("Ana")
        val habitId = habit("Steps", HabitType.NUMBER, targetValue = 10000.0, unit = "steps")
        val today = LocalDate.now()
        repo.saveDailyHabitEntry(userId, today, habitId, HabitType.NUMBER, numericValue = 8500.0)
        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(8500.0, entries.first().numericValue)
    }

    @Test
    fun saveDailyHabitEntry_text_persisted() = runBlocking {
        val userId = repo.createUser("Leo")
        val habitId = habit("Gratitude", HabitType.TEXT)
        val today = LocalDate.now()
        repo.saveDailyHabitEntry(userId, today, habitId, HabitType.TEXT, textValue = "Grateful for family")
        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals("Grateful for family", entries.first().textValue)
    }

    @Test
    fun updateEntryQuality_persisted() = runBlocking {
        val userId = repo.createUser("Mia")
        val habitId = habit("Read")
        val today = LocalDate.now()
        repo.toggleBooleanHabit(userId, habitId, today)
        repo.updateEntryQuality(userId, habitId, today, quality = 3)
        val entries = db.database.habitTrackingDao().getEntriesForHabitInRange(userId, habitId, today, today)
        assertEquals(3, entries.first().quality)
        assertEquals(true, entries.first().booleanValue)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test
    fun setHabitLifecycle_paused_excludedFromDailyItems() = runBlocking {
        val userId = repo.createUser("Tom")
        val habitId = habit("Meditate")
        repo.replaceAssignmentsForUser(userId, listOf(habitId))
        val h = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(h, HabitLifecycleStatus.PAUSED)
        val items = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertTrue("PAUSED habit must not appear in daily items", items.isEmpty())
    }

    @Test
    fun setHabitLifecycle_graduated_appearsInGraduatedList() = runBlocking {
        val userId = repo.createUser("Zoe")
        val habitId = habit("No Sugar")
        repo.replaceAssignmentsForUser(userId, listOf(habitId))
        val h = db.database.habitTrackingDao().getHabitById(habitId)!!
        repo.setHabitLifecycle(h, HabitLifecycleStatus.GRADUATED)
        val graduated = repo.getGraduatedHabitsForUser(userId).first()
        assertEquals(1, graduated.size)
        val daily = repo.getDailyHabitItems(userId, LocalDate.now()).first()
        assertTrue("GRADUATED habit must not appear in daily items", daily.isEmpty())
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    fun resetAllData_allTablesEmpty() = runBlocking {
        repo.createUser("Ghost")
        habit("Run")
        repo.resetAllData()
        val users = db.database.userDao().getAllUsers().first()
        val habits = db.database.habitTrackingDao().getAllHabits().first()
        assertTrue(users.isEmpty())
        // After reset, only built-in chants are reseeded — habits must be empty
        assertTrue(habits.isEmpty())
    }

    @Test
    fun clearForRestore_tablesEmpty() = runBlocking {
        repo.createUser("Temp")
        repo.clearForRestore()
        val users = db.database.userDao().getAllUsers().first()
        assertTrue(users.isEmpty())
    }

    // ── Bulk entries ──────────────────────────────────────────────────────────

    @Test
    fun importBulkEntries_allPersisted() = runBlocking {
        val userId = repo.createUser("Bulk")
        val habitId = habit("Habit")
        val start = LocalDate.of(2024, 1, 1)
        val entries = (0L until 1000L).map { offset ->
            DailyHabitEntry(userId = userId, habitId = habitId, date = start.plusDays(offset), booleanValue = true)
        }
        repo.importBulkEntries(entries)
        val all = db.database.habitTrackingDao().getAllEntriesForUser(userId)
        assertEquals(1000, all.size)
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    @Test
    fun getHabitStreak_7dayDailyHabit() = runBlocking {
        val userId = repo.createUser("Streak7")
        val habitId = habit("Daily7")
        val today = LocalDate.now()
        for (i in 0L..6L) repo.toggleBooleanHabit(userId, habitId, today.minusDays(i))
        val streak = repo.getHabitStreak(userId, habitId, HabitType.BOOLEAN, null)
        assertEquals(7, streak)
    }

    @Test
    fun getHabitStreak_breaksOnMiss() = runBlocking {
        val userId = repo.createUser("StreakBreak")
        val habitId = habit("Breakable")
        val today = LocalDate.now()
        repo.toggleBooleanHabit(userId, habitId, today)
        repo.toggleBooleanHabit(userId, habitId, today.minusDays(1))
        // day -2 skipped intentionally
        repo.toggleBooleanHabit(userId, habitId, today.minusDays(3))
        val streak = repo.getHabitStreak(userId, habitId, HabitType.BOOLEAN, null)
        assertEquals(2, streak)
    }
}
