package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habitpower.data.model.DailyHealthStat
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyHealthStatDao {
    @Query("SELECT * FROM daily_health_stats WHERE date = :date")
    fun getStatForDate(date: LocalDate): Flow<DailyHealthStat?>

    @Query("SELECT * FROM daily_health_stats WHERE date BETWEEN :startDate AND :endDate")
    fun getStatsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthStat>>

    @Query("SELECT * FROM daily_health_stats")
    fun getAllStats(): Flow<List<DailyHealthStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: DailyHealthStat)
}
