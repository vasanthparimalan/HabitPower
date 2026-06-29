package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.UserLifeAreaAssignment
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifeAreaDaoTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val dao get() = db.database.lifeAreaDao()
    private val userDao get() = db.database.userDao()

    @Test
    fun insertLifeArea_retrieveAll() = runBlocking {
        dao.insertLifeArea(LifeArea(name = "Health", emoji = "💪"))
        dao.insertLifeArea(LifeArea(name = "Career", emoji = "💼"))
        dao.insertLifeArea(LifeArea(name = "Relationships", emoji = "❤️"))

        val all = dao.getAllLifeAreas().firstBlocking()
        assertEquals(3, all.size)
        assertTrue(all.any { it.name == "Health" })
        assertTrue(all.any { it.name == "Career" })
    }

    @Test
    fun assignLifeAreaToUser_appearsInUserList() = runBlocking {
        val userId = userDao.insertUser(UserProfile(name = "Alice"))
        val areaId = dao.insertLifeArea(LifeArea(name = "Fitness"))
        dao.upsertLifeAreaAssignment(UserLifeAreaAssignment(userId = userId, lifeAreaId = areaId))

        val assigned = dao.getAssignedLifeAreasForUser(userId).firstBlocking()
        assertEquals(1, assigned.size)
        assertEquals("Fitness", assigned.first().name)
    }
}
