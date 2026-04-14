package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.UserHabitAssignment
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitTrackingDao {
    @Query("SELECT * FROM habit_definitions WHERE isActive = 1 ORDER BY displayOrder ASC, name ASC")
    fun getActiveHabits(): Flow<List<HabitDefinition>>

    @Query("SELECT * FROM habit_definitions ORDER BY displayOrder ASC, name ASC")
    fun getAllHabits(): Flow<List<HabitDefinition>>

    @Query("SELECT * FROM habit_definitions WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): HabitDefinition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitDefinition): Long

    @Update
    suspend fun updateHabit(habit: HabitDefinition)

    @Delete
    suspend fun deleteHabit(habit: HabitDefinition)

    @Query(
        """
        SELECT hd.*
        FROM habit_definitions hd
        INNER JOIN user_habit_assignments ua ON ua.habitId = hd.id
        WHERE ua.userId = :userId AND ua.isActive = 1 AND hd.isActive = 1
        ORDER BY ua.displayOrder ASC, hd.displayOrder ASC, hd.name ASC
        """
    )
    fun getAssignedHabitsForUser(userId: Long): Flow<List<HabitDefinition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(assignment: UserHabitAssignment)

    @Query("DELETE FROM user_habit_assignments WHERE userId = :userId")
    suspend fun clearAssignmentsForUser(userId: Long)

    @Query("SELECT habitId FROM user_habit_assignments WHERE userId = :userId AND isActive = 1 ORDER BY displayOrder ASC")
    fun getAssignedHabitIdsForUser(userId: Long): Flow<List<Long>>

    @Query(
        """
        SELECT
            hd.id AS habitId,
            hd.routineId AS routineId,
            hd.lifeAreaId AS lifeAreaId,
            hd.name AS name,
            hd.goalIdentityStatement AS goalIdentityStatement,
            hd.description AS description,
            hd.type AS type,
            hd.unit AS unit,
            hd.targetValue AS targetValue,
            hd.showInWidget AS showInWidget,
            hd.showInDailyCheckIn AS showInDailyCheckIn,
            hd.displayOrder AS habitDisplayOrder,
            hd.operator AS operator,
            hd.recurrenceType AS recurrenceType,
            hd.recurrenceInterval AS recurrenceInterval,
            hd.recurrenceDaysOfWeekMask AS recurrenceDaysOfWeekMask,
            hd.recurrenceDayOfMonth AS recurrenceDayOfMonth,
            hd.recurrenceWeekOfMonth AS recurrenceWeekOfMonth,
            hd.recurrenceWeekday AS recurrenceWeekday,
            hd.recurrenceYearlyDates AS recurrenceYearlyDates,
            hd.recurrenceAnchorDate AS recurrenceAnchorDate,
            hd.recurrenceStartDate AS recurrenceStartDate,
            hd.recurrenceEndDate AS recurrenceEndDate,
            ua.displayOrder AS assignmentDisplayOrder,
            dhe.booleanValue AS entryBooleanValue,
            dhe.numericValue AS entryNumericValue,
            dhe.textValue AS entryTextValue,
            hd.commitmentTime AS commitmentTime,
            hd.commitmentLocation AS commitmentLocation
        FROM user_habit_assignments ua
        INNER JOIN habit_definitions hd ON hd.id = ua.habitId
        LEFT JOIN daily_habit_entries dhe
            ON dhe.userId = ua.userId
            AND dhe.habitId = ua.habitId
            AND dhe.date = :date
        WHERE ua.userId = :userId
            AND ua.isActive = 1
            AND hd.isActive = 1
        ORDER BY ua.displayOrder ASC, hd.displayOrder ASC, hd.name ASC
        """
    )
    fun getDailyHabitItems(userId: Long, date: LocalDate): Flow<List<DailyHabitItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyEntry(entry: DailyHabitEntry)

    @Query(
        """
        DELETE FROM daily_habit_entries
        WHERE userId = :userId AND habitId = :habitId AND date = :date
        """
    )
    suspend fun deleteDailyEntry(userId: Long, habitId: Long, date: LocalDate)

    @Query("SELECT * FROM daily_habit_entries WHERE userId = :userId AND habitId = :habitId ORDER BY date DESC")
    suspend fun getEntriesForHabitDesc(userId: Long, habitId: Long): List<DailyHabitEntry>

    @Query("SELECT * FROM daily_habit_entries WHERE userId = :userId AND habitId = :habitId AND date >= :from AND date <= :to ORDER BY date DESC")
    suspend fun getEntriesForHabitInRange(userId: Long, habitId: Long, from: LocalDate, to: LocalDate): List<DailyHabitEntry>

    @Query("SELECT * FROM daily_habit_entries WHERE userId = :userId AND date >= :from AND date <= :to ORDER BY date DESC")
    fun getEntriesForUserInRange(userId: Long, from: LocalDate, to: LocalDate): Flow<List<DailyHabitEntry>>

    @Query("SELECT * FROM daily_habit_entries WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllEntriesForUser(userId: Long): List<DailyHabitEntry>

    @Query(
        """
        SELECT hd.*
        FROM habit_definitions hd
        INNER JOIN user_habit_assignments ua ON ua.habitId = hd.id
        WHERE ua.userId = :userId AND ua.isActive = 1 AND hd.lifecycleStatus = 'GRADUATED'
        ORDER BY hd.name ASC
        """
    )
    fun getGraduatedHabitsForUser(userId: Long): Flow<List<HabitDefinition>>
}
