package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.habitpower.data.model.PomodoroSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PomodoroSessionDao {
    @Insert
    suspend fun insert(session: PomodoroSession): Long

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND date = :date AND linkedHabitId IS NULL ORDER BY completedAt DESC")
    fun getUnlinkedSessionsForDate(userId: Long, date: LocalDate): Flow<List<PomodoroSession>>

    @Query("UPDATE pomodoro_sessions SET linkedHabitId = :habitId WHERE id = :sessionId")
    suspend fun linkToHabit(sessionId: Long, habitId: Long)

    @Delete
    suspend fun delete(session: PomodoroSession)

    @Query("DELETE FROM pomodoro_sessions WHERE userId = :userId AND date < :before AND linkedHabitId IS NULL")
    suspend fun deleteOldUnlinked(userId: Long, before: LocalDate)
}
