package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): Routine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExerciseCrossRef(crossRef: RoutineExerciseCrossRef)

    @Query("DELETE FROM routine_exercise_cross_ref WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)

    @Transaction
    @Query("SELECT exercises.* FROM exercises INNER JOIN routine_exercise_cross_ref ON exercises.id = routine_exercise_cross_ref.exerciseId WHERE routine_exercise_cross_ref.routineId = :routineId ORDER BY routine_exercise_cross_ref.`order` ASC")
    fun getExercisesForRoutine(routineId: Long): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM routine_exercise_cross_ref WHERE routineId = :routineId")
    fun getExerciseCountForRoutine(routineId: Long): Flow<Int>
}
