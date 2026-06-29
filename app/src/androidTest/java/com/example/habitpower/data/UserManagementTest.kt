package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UserManagementTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    @Test
    fun createMultipleUsers_switchActive_itemsChangePerUser() = runBlocking {
        val user1 = repo.createUser("Alice")
        val user2 = repo.createUser("Bob")

        val habitA = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "HabitA", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        val habitB = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "HabitB", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )

        repo.replaceAssignmentsForUser(user1, listOf(habitA))
        repo.replaceAssignmentsForUser(user2, listOf(habitB))

        val user1Items = repo.getDailyHabitItems(user1, LocalDate.now()).first()
        val user2Items = repo.getDailyHabitItems(user2, LocalDate.now()).first()

        assertEquals(1, user1Items.size)
        assertEquals("HabitA", user1Items.first().name)

        assertEquals(1, user2Items.size)
        assertEquals("HabitB", user2Items.first().name)
    }

    @Test
    fun deleteUser_assignmentsGone() = runBlocking {
        val userId = repo.createUser("Dave")
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "H", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        repo.replaceAssignmentsForUser(userId, listOf(habitId))

        val user = db.database.userDao().getUserById(userId)!!
        repo.deleteUser(user)

        val assignments = db.database.habitTrackingDao().getAllAssignments()
        assertTrue(assignments.none { it.userId == userId })
    }

    @Test
    fun deleteUser_entriesGone() = runBlocking {
        val userId = repo.createUser("Eve")
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Run", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        repo.toggleBooleanHabit(userId, habitId, LocalDate.now())

        val user = db.database.userDao().getUserById(userId)!!
        repo.deleteUser(user)

        val entries = db.database.habitTrackingDao().getAllEntriesForUser(userId)
        assertTrue(entries.isEmpty())
    }
}
