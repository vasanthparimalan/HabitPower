package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.UserLifeAreaAssignment
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeAreaDao {
    @Query("SELECT * FROM life_areas WHERE isActive = 1 ORDER BY displayOrder ASC, name ASC")
    fun getActiveLifeAreas(): Flow<List<LifeArea>>

    @Query("SELECT * FROM life_areas ORDER BY displayOrder ASC, name ASC")
    fun getAllLifeAreas(): Flow<List<LifeArea>>

    @Query(
        """
        SELECT la.*
        FROM life_areas la
        INNER JOIN user_life_area_assignments ula ON ula.lifeAreaId = la.id
        WHERE ula.userId = :userId AND ula.isActive = 1 AND la.isActive = 1
        ORDER BY ula.displayOrder ASC, la.displayOrder ASC, la.name ASC
        """
    )
    fun getAssignedLifeAreasForUser(userId: Long): Flow<List<LifeArea>>

    @Query(
        """
        SELECT lifeAreaId
        FROM user_life_area_assignments
        WHERE userId = :userId AND isActive = 1
        ORDER BY displayOrder ASC
        """
    )
    fun getAssignedLifeAreaIdsForUser(userId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifeArea(lifeArea: LifeArea): Long

    @Update
    suspend fun updateLifeArea(lifeArea: LifeArea)

    @Delete
    suspend fun deleteLifeArea(lifeArea: LifeArea)

    @Query("SELECT * FROM life_areas WHERE id = :id LIMIT 1")
    suspend fun getLifeAreaById(id: Long): LifeArea?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLifeAreaAssignment(assignment: UserLifeAreaAssignment)

    @Query("DELETE FROM user_life_area_assignments WHERE userId = :userId")
    suspend fun clearLifeAreaAssignmentsForUser(userId: Long)
}
