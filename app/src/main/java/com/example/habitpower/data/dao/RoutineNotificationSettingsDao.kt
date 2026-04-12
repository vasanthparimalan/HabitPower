package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habitpower.data.model.RoutineNotificationSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineNotificationSettingsDao {
    @Query("SELECT * FROM routine_notification_settings LIMIT 1")
    fun getSettings(): Flow<RoutineNotificationSettings?>

    @Query("SELECT * FROM routine_notification_settings LIMIT 1")
    suspend fun getSettingsSuspend(): RoutineNotificationSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: RoutineNotificationSettings)

    @Update
    suspend fun updateSettings(settings: RoutineNotificationSettings)

    @Delete
    suspend fun deleteSettings(settings: RoutineNotificationSettings)
}
