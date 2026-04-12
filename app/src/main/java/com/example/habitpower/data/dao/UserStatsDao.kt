package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habitpower.data.model.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    fun observeStats(userId: Long): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    suspend fun getStats(userId: Long): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: UserStats)

    @Query("DELETE FROM user_stats WHERE userId = :userId")
    suspend fun deleteStats(userId: Long)
}
