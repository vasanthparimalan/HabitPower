package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habitpower.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE date = :date")
    fun getSessionsForDate(date: LocalDate): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :startDate AND :endDate")
    fun getSessionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY date DESC")
    suspend fun getAllCompletedSessions(): List<WorkoutSession>

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}
