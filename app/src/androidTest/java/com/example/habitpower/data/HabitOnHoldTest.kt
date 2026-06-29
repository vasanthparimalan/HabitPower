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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HabitOnHoldTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    private val today: LocalDate = LocalDate.now()
    private val yesterday: LocalDate = today.minusDays(1)
    private val tomorrow: LocalDate = today.plusDays(1)
    private val nextWeek: LocalDate = today.plusWeeks(1)

    /** Creates a user, inserts a habit, assigns it, and returns (userId, habit). */
    private suspend fun setupUserAndHabit(
        habitName: String = "Test Habit"
    ): Pair<Long, HabitDefinition> {
        val userId = db.database.userDao().insertUser(UserProfile(name = "User"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = habitName, goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, habitId))
        val habit = db.database.habitTrackingDao().getHabitById(habitId)!!
        return userId to habit
    }

    // ── putHabitOnHold ────────────────────────────────────────────────────────

    @Test
    fun putOnHold_timeBound_setsCorrectFields() = runBlocking {
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, nextWeek)

        val updated = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.PAUSED, updated.lifecycleStatus)
        assertEquals(false, updated.isActive)
        assertEquals(nextWeek, updated.pausedUntil)
    }

    @Test
    fun putOnHold_indefinite_setsCorrectFields() = runBlocking {
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, null)

        val updated = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.PAUSED, updated.lifecycleStatus)
        assertEquals(false, updated.isActive)
        assertNull(updated.pausedUntil)
    }

    @Test
    fun timeBoundHold_hiddenFromDailyItems() = runBlocking {
        val (userId, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, nextWeek)

        val items = repo.getDailyHabitItems(userId, today).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun indefiniteHold_hiddenFromDailyItems() = runBlocking {
        val (userId, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, null)

        val items = repo.getDailyHabitItems(userId, today).first()
        assertTrue(items.isEmpty())
    }

    // ── resumeHabitFromHold ───────────────────────────────────────────────────

    @Test
    fun resumeFromHold_restoresActiveState() = runBlocking {
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, nextWeek)
        val onHold = db.database.habitTrackingDao().getHabitById(habit.id)!!

        repo.resumeHabitFromHold(onHold)

        val resumed = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.ACTIVE, resumed.lifecycleStatus)
        assertEquals(true, resumed.isActive)
        assertNull(resumed.pausedUntil)
    }

    @Test
    fun resumedHabit_reappearsInDailyItems() = runBlocking {
        val (userId, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, nextWeek)
        val onHold = db.database.habitTrackingDao().getHabitById(habit.id)!!
        repo.resumeHabitFromHold(onHold)

        val items = repo.getDailyHabitItems(userId, today).first()
        assertEquals(1, items.size)
    }

    // ── getOnHoldHabitsForUser ────────────────────────────────────────────────

    @Test
    fun getOnHoldHabits_timeBoundOnly_excludesIndefinitePause() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "User"))

        fun addHabit(name: String): HabitDefinition {
            val id = runBlocking {
                db.database.habitTrackingDao().insertHabit(
                    HabitDefinition(name = name, goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
                )
            }
            runBlocking { db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, id)) }
            return runBlocking { db.database.habitTrackingDao().getHabitById(id)!! }
        }

        // time-bound hold → should appear in On Hold list
        val timeBound = addHabit("Time-bound")
        repo.putHabitOnHold(timeBound, nextWeek)

        // indefinite hold (null) → should NOT appear
        val indefinite = addHabit("Indefinite")
        repo.putHabitOnHold(indefinite, null)

        // still active → should NOT appear
        addHabit("Active")

        val onHold = repo.getOnHoldHabitsForUser(userId).first()
        assertEquals(1, onHold.size)
        assertEquals("Time-bound", onHold[0].name)
    }

    @Test
    fun getOnHoldHabits_sortedByReturnDate() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "User"))

        // Insert the habit with the later return date first to confirm sort is by date, not insertion order.
        val idLater = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Returns Later", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, idLater))
        val hLater = db.database.habitTrackingDao().getHabitById(idLater)!!
        repo.putHabitOnHold(hLater, nextWeek)

        val idSooner = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Returns Sooner", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, idSooner))
        val hSooner = db.database.habitTrackingDao().getHabitById(idSooner)!!
        repo.putHabitOnHold(hSooner, tomorrow)

        val onHold = repo.getOnHoldHabitsForUser(userId).first()
        assertEquals(2, onHold.size)
        assertEquals("Returns Sooner", onHold[0].name)
        assertEquals("Returns Later", onHold[1].name)
    }

    // ── autoResumeExpiredHolds ────────────────────────────────────────────────

    @Test
    fun autoResume_expiredHold_resumesHabit() = runBlocking {
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, yesterday)

        // Call the DAO directly to control the "today" date.
        db.database.habitTrackingDao().autoResumeExpiredHolds(today)

        val resumed = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.ACTIVE, resumed.lifecycleStatus)
        assertEquals(true, resumed.isActive)
        assertNull(resumed.pausedUntil)
    }

    @Test
    fun autoResume_holdEndingToday_notResumedYet() = runBlocking {
        // pausedUntil = today is NOT strictly less-than today, so the hold is still active.
        // The habit resumes on tomorrow's launch.
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, today)

        db.database.habitTrackingDao().autoResumeExpiredHolds(today)

        val stillHeld = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.PAUSED, stillHeld.lifecycleStatus)
        assertNotNull(stillHeld.pausedUntil)
    }

    @Test
    fun autoResume_futureHold_notResumed() = runBlocking {
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, tomorrow)

        db.database.habitTrackingDao().autoResumeExpiredHolds(today)

        val stillHeld = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.PAUSED, stillHeld.lifecycleStatus)
        assertEquals(tomorrow, stillHeld.pausedUntil)
    }

    @Test
    fun autoResume_indefiniteHold_neverAutoResumes() = runBlocking {
        // Indefinite holds have pausedUntil = NULL. The auto-resume query filters
        // `pausedUntil IS NOT NULL`, so they are never touched regardless of date.
        val (_, habit) = setupUserAndHabit()
        repo.putHabitOnHold(habit, null)

        db.database.habitTrackingDao().autoResumeExpiredHolds(today.plusYears(100))

        val stillHeld = db.database.habitTrackingDao().getHabitById(habit.id)!!
        assertEquals(HabitLifecycleStatus.PAUSED, stillHeld.lifecycleStatus)
        assertNull(stillHeld.pausedUntil)
    }
}
