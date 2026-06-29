package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.ChantSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChantDao {
    @Query("SELECT * FROM chant_definitions ORDER BY isBuiltIn DESC, name ASC")
    fun getAllChants(): Flow<List<ChantDefinition>>

    @Query("SELECT * FROM chant_definitions ORDER BY isBuiltIn DESC, name ASC")
    suspend fun getAllChantsSync(): List<ChantDefinition>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChant(chant: ChantDefinition): Long

    @Update
    suspend fun updateChant(chant: ChantDefinition)

    @Delete
    suspend fun deleteChant(chant: ChantDefinition)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChantSession): Long

    @Query("SELECT * FROM chant_sessions WHERE userId = :userId ORDER BY completedAt DESC LIMIT 20")
    fun getRecentSessions(userId: Long): Flow<List<ChantSession>>
}
