package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.WorkoutSession
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
class WorkoutAndHealthTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val workoutDao get() = db.database.workoutSessionDao()
    private val healthDao get() = db.database.dailyHealthStatDao()

    @Test
    fun insertWorkoutSession_retrieveByDate() = runBlocking {
        val today = LocalDate.now()
        workoutDao.insertSession(
            WorkoutSession(
                date = today,
                routineId = 1L,
                routineName = "Chest Day",
                isCompleted = true,
                startTime = System.currentTimeMillis(),
                endTime = null
            )
        )

        val sessions = workoutDao.getSessionsForDate(today).first()
        assertEquals(1, sessions.size)
        assertEquals("Chest Day", sessions.first().routineName)
    }

    @Test
    fun deleteWorkoutSession_gone() = runBlocking {
        val today = LocalDate.now()
        workoutDao.insertSession(
            WorkoutSession(
                date = today, routineId = 2L, routineName = "Leg Day",
                isCompleted = false, startTime = System.currentTimeMillis(), endTime = null
            )
        )
        val session = workoutDao.getSessionsForDate(today).first().first()
        workoutDao.deleteSession(session.id)

        val after = workoutDao.getSessionsForDate(today).first()
        assertTrue(after.isEmpty())
    }

    @Test
    fun saveDailyStat_retrieveByDate() = runBlocking {
        val date = LocalDate.of(2026, 6, 10)
        healthDao.insertOrUpdate(DailyHealthStat(date = date, sleepHours = 7.5f, stepsCount = 9000))

        val stat = healthDao.getStatForDate(date).first()
        assertNotNull(stat)
        assertEquals(7.5f, stat!!.sleepHours)
        assertEquals(9000, stat.stepsCount)
    }

    @Test
    fun getStatsForDateRange_boundariesIncluded() = runBlocking {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 7)
        val outsideRange = LocalDate.of(2026, 6, 8)

        for (i in 0L..6L) {
            healthDao.insertOrUpdate(DailyHealthStat(date = start.plusDays(i), sleepHours = 7f, stepsCount = 5000))
        }
        healthDao.insertOrUpdate(DailyHealthStat(date = outsideRange, sleepHours = 8f, stepsCount = 6000))

        val stats = healthDao.getStatsForDateRange(start, end).first()
        assertEquals(7, stats.size)
        assertTrue(stats.none { it.date == outsideRange })
    }
}
