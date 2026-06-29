package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habitpower.data.model.MeditationSession
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MeditationSession): Long

    @Query("SELECT COUNT(*) FROM meditation_sessions WHERE userId = :userId AND completedAt >= :fromMs AND completedAt < :toMs")
    fun countSessionsInRange(userId: Long, fromMs: Long, toMs: Long): Flow<Int>
}
