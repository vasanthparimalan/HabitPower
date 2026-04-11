package com.example.habitpower.data.repository

import com.example.habitpower.data.dao.LifeAreaDao
import com.example.habitpower.data.model.LifeArea
import kotlinx.coroutines.flow.Flow

/**
 * Thin repository focused on LifeArea domain operations.
 * Keeps the data access surface small and testable.
 */
class LifeAreaRepository(private val dao: LifeAreaDao) {
    fun getAll(): Flow<List<LifeArea>> = dao.getAllLifeAreas()
    fun getActive(): Flow<List<LifeArea>> = dao.getActiveLifeAreas()
    suspend fun create(area: LifeArea): Long = dao.insertLifeArea(area)
    suspend fun update(area: LifeArea) = dao.updateLifeArea(area)
    suspend fun delete(area: LifeArea) = dao.deleteLifeArea(area)
}
