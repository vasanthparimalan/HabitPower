package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val userDao get() = db.database.userDao()
    private val habitDao get() = db.database.habitTrackingDao()

    @Test
    fun insertUser_retrieveById() = runBlocking {
        val id = userDao.insertUser(UserProfile(name = "Alice"))
        val user = userDao.getUserById(id)
        assertNotNull(user)
        assertEquals("Alice", user!!.name)
        assertTrue(user.isActive)
    }

    @Test
    fun updateUser_nameChange_persisted() = runBlocking {
        val id = userDao.insertUser(UserProfile(name = "Bob"))
        val user = userDao.getUserById(id)!!
        userDao.updateUser(user.copy(name = "Robert"))
        val updated = userDao.getUserById(id)
        assertEquals("Robert", updated?.name)
    }

    @Test
    fun deleteUser_entriesCascadeDeleted() = runBlocking {
        val userId = userDao.insertUser(UserProfile(name = "Carol"))
        val habitId = habitDao.insertHabit(
            HabitDefinition(name = "Read", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        habitDao.upsertDailyEntry(
            DailyHabitEntry(userId = userId, habitId = habitId, date = LocalDate.now(), booleanValue = true)
        )

        val user = userDao.getUserById(userId)!!
        userDao.deleteUser(user)

        val entries = habitDao.getAllEntriesForUser(userId)
        assertTrue("Entries must cascade-delete when user is deleted", entries.isEmpty())
    }

    @Test
    fun deleteUser_assignmentsCascadeDeleted() = runBlocking {
        val userId = userDao.insertUser(UserProfile(name = "Dave"))
        val habitId = habitDao.insertHabit(
            HabitDefinition(name = "Meditate", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        habitDao.upsertAssignment(UserHabitAssignment(userId = userId, habitId = habitId))

        val user = userDao.getUserById(userId)!!
        userDao.deleteUser(user)

        val assignments = habitDao.getAllAssignments()
        assertTrue(
            "Assignments must cascade-delete when user is deleted",
            assignments.none { it.userId == userId }
        )
    }

    @Test
    fun multipleUsers_getAllReturnsAll() = runBlocking {
        userDao.insertUser(UserProfile(name = "Eve"))
        userDao.insertUser(UserProfile(name = "Frank"))
        userDao.insertUser(UserProfile(name = "Grace"))

        val all = db.database.userDao().getAllUsers().firstBlocking()
        assertEquals(3, all.size)
    }
}
