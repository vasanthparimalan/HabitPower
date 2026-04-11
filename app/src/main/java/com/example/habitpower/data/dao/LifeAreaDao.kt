package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habitpower.data.model.LifeArea
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeAreaDao {
    @Query("SELECT * FROM life_areas WHERE isActive = 1 ORDER BY displayOrder ASC, name ASC")
    fun getActiveLifeAreas(): Flow<List<LifeArea>>

    @Query("SELECT * FROM life_areas ORDER BY displayOrder ASC, name ASC")
    fun getAllLifeAreas(): Flow<List<LifeArea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifeArea(lifeArea: LifeArea): Long

    @Update
    suspend fun updateLifeArea(lifeArea: LifeArea)

    @Delete
    suspend fun deleteLifeArea(lifeArea: LifeArea)

    @Query("SELECT * FROM life_areas WHERE id = :id LIMIT 1")
    suspend fun getLifeAreaById(id: Long): LifeArea?
}
