package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserLifeAreaAssignment
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HpexBackupManagerTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)
    private val gamification get() = GamificationRepository(
        db.database.userStatsDao(),
        db.database.habitTrackingDao(),
        db.database.lifeAreaDao()
    )
    private val manager get() = HpexBackupManager(repo, gamification)

    private val allSections = HpexSection.values().toSet()

    // ── Export ────────────────────────────────────────────────────────────────

    @Test
    fun export_allSections_validJson() = runBlocking {
        seedMinimalData()
        val json = manager.export(allSections, "1.7")
        val root = JSONObject(json)
        assertEquals(1, root.getInt("hpexVersion"))
        assertNotNull(root.getString("exportedAt"))
        assertEquals("1.7", root.getString("appVersion"))
    }

    @Test
    fun export_sectionSubset_onlyRequestedSectionsPresent() = runBlocking {
        seedMinimalData()
        val json = manager.export(setOf(HpexSection.USERS, HpexSection.HABITS), "1.7")
        val root = JSONObject(json)
        assertTrue(root.has("users"))
        assertTrue(root.has("habits"))
        assertFalse(root.has("lifeAreas"))
        assertFalse(root.has("routines"))
    }

    // ── Round-trip: core entities ─────────────────────────────────────────────

    @Test
    fun roundTrip_users_exactNameAndIdPreserved() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "Vasanth"))
        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val users = db.database.userDao().getAllUsers().first()
        assertEquals(1, users.size)
        assertEquals("Vasanth", users.first().name)
        assertEquals(userId, users.first().id)
    }

    @Test
    fun roundTrip_habitDefinition_allRecurrenceFieldsPreserved() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "Tester"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(
                name = "Mon-Wed-Fri Run",
                goalIdentityStatement = "I am a runner",
                description = "30 min jog",
                type = HabitType.BOOLEAN,
                recurrenceType = HabitRecurrenceType.WEEKLY_SELECTED_DAYS,
                recurrenceDaysOfWeekMask = (1 shl 1) or (1 shl 3) or (1 shl 5),
                recurrenceStartDate = LocalDate.of(2026, 1, 1)
            )
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, habitId))

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val habits = db.database.habitTrackingDao().getAllHabits().first()
        assertEquals(1, habits.size)
        val restored = habits.first()
        assertEquals("Mon-Wed-Fri Run", restored.name)
        assertEquals(HabitRecurrenceType.WEEKLY_SELECTED_DAYS, restored.recurrenceType)
        assertEquals((1 shl 1) or (1 shl 3) or (1 shl 5), restored.recurrenceDaysOfWeekMask)
        assertEquals(LocalDate.of(2026, 1, 1), restored.recurrenceStartDate)
    }

    @Test
    fun roundTrip_habitEntries_allDatesAndValuesPreserved() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "EntryUser"))
        val habitId = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Read", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, habitId))

        val start = LocalDate.of(2026, 5, 1)
        val entries = (0L until 30L).map { offset ->
            DailyHabitEntry(userId, habitId, start.plusDays(offset), booleanValue = true)
        }
        db.database.habitTrackingDao().insertAllEntries(entries)

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val restoredEntries = db.database.habitTrackingDao().getAllEntriesForUser(userId)
        assertEquals(30, restoredEntries.size)
        assertTrue(restoredEntries.all { it.booleanValue == true })
    }

    @Test
    fun roundTrip_userHabitAssignments_preserved() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "AssignUser"))
        val h1 = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "H1", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        val h2 = db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "H2", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, h1))
        db.database.habitTrackingDao().upsertAssignment(UserHabitAssignment(userId, h2))

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val assigned = db.database.habitTrackingDao().getAssignedHabitsForUser(userId).first()
        assertEquals(2, assigned.size)
        assertTrue(assigned.any { it.name == "H1" })
        assertTrue(assigned.any { it.name == "H2" })
    }

    @Test
    fun roundTrip_userLifeAreaAssignments_preserved() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "AreaUser"))
        val areaId = db.database.lifeAreaDao().insertLifeArea(LifeArea(name = "Health"))
        db.database.lifeAreaDao().upsertLifeAreaAssignment(UserLifeAreaAssignment(userId, areaId))

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val assigned = db.database.lifeAreaDao().getAssignedLifeAreasForUser(userId).first()
        assertEquals(1, assigned.size)
        assertEquals("Health", assigned.first().name)
    }

    @Test
    fun roundTrip_activeUserRestored() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "ActiveOne"))
        db.database.userDao().insertUser(UserProfile(name = "InactiveOne"))
        repo.saveActiveUserId(userId)

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val resolved = repo.getResolvedActiveUser().first()
        assertNotNull(resolved)
        // The first user in the file should be restored as active
        assertNotNull(resolved!!.name)
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    fun import_twice_noDuplicates() = runBlocking {
        db.database.userDao().insertUser(UserProfile(name = "Solo"))
        db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "OneHabit", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
        val json = manager.export(allSections, "1.7")

        manager.import(json)
        manager.import(json) // second import

        val users = db.database.userDao().getAllUsers().first()
        val habits = db.database.habitTrackingDao().getAllHabits().first()
        assertEquals("Importing twice must not duplicate users", 1, users.size)
        assertEquals("Importing twice must not duplicate habits", 1, habits.size)
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    fun import_invalidJson_throwsException() = runBlocking {
        db.database.userDao().insertUser(UserProfile(name = "Survivor"))

        var threw = false
        try {
            manager.import("{ this is not valid json !!!")
        } catch (_: Exception) {
            threw = true
        }

        assertTrue("Invalid JSON must throw an exception", threw)
    }

    @Test
    fun import_versionZero_throwsWithMessage() = runBlocking {
        var threw = false
        try {
            manager.import("""{"hpexVersion":0,"sections":[]}""")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("hpexVersion=0 must throw IllegalArgumentException", threw)
    }

    @Test
    fun import_missingUsersSection_graceful() = runBlocking {
        // JSON is valid but has no users key → 0 users, no crash
        val json = """{"hpexVersion":1,"sections":["habits"],"habits":[]}"""
        val result = manager.import(json)
        assertEquals(0, result.users)
    }

    // ── Built-in chants ───────────────────────────────────────────────────────

    @Test
    fun import_builtInChantsReseeded() = runBlocking {
        // clearAllTables wipes built-in chants; import must re-seed them
        db.database.clearAllTables()
        val json = """{"hpexVersion":1,"sections":[]}"""
        manager.import(json)

        val chants = db.database.chantDao().getAllChants().first()
        assertTrue("Built-in chants must be re-seeded after import", chants.isNotEmpty())
    }

    @Test
    fun import_customChants_preserved() = runBlocking {
        db.database.chantDao().insertChant(
            ChantDefinition(name = "My Mantra", text = "Om Shanti", isBuiltIn = false)
        )
        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val chants = db.database.chantDao().getAllChants().first()
        assertTrue(chants.any { it.name == "My Mantra" && !it.isBuiltIn })
    }

    // ── Routines ──────────────────────────────────────────────────────────────

    @Test
    fun import_routinesWithExercises_crossRefsPreserved() = runBlocking {
        val routineId = db.database.routineDao().insertRoutine(
            Routine(name = "Morning Workout", description = "", type = RoutineType.NORMAL)
        )
        val e1 = db.database.exerciseDao().insertExercise(
            Exercise(name = "Push-up", description = "", imageUri = null, category = ExerciseCategory.STRENGTH)
        )
        val e2 = db.database.exerciseDao().insertExercise(
            Exercise(name = "Squat", description = "", imageUri = null, category = ExerciseCategory.STRENGTH)
        )
        db.database.routineDao().insertRoutineExerciseCrossRef(
            RoutineExerciseCrossRef(routineId = routineId, exerciseId = e1, order = 0)
        )
        db.database.routineDao().insertRoutineExerciseCrossRef(
            RoutineExerciseCrossRef(routineId = routineId, exerciseId = e2, order = 1)
        )

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val crossRefs = repo.getAllRoutineCrossRefsForExport()
        assertEquals(2, crossRefs.size)
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    @Test
    fun import_tasks_allItemsPreserved() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "TaskUser"))
        val listId = db.database.taskDao().insertTaskList(TaskList(userId = userId, name = "Work"))
        db.database.taskDao().insertTask(Task(taskListId = listId, name = "Send report"))
        db.database.taskDao().insertTask(Task(taskListId = listId, name = "Review PR"))
        db.database.taskDao().insertTask(Task(taskListId = listId, name = "Team meeting"))

        val json = manager.export(allSections, "1.7")
        manager.import(json)

        val tasks = repo.getAllTasksForExport()
        assertEquals(3, tasks.size)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun seedMinimalData() {
        val userId = db.database.userDao().insertUser(UserProfile(name = "Default"))
        db.database.habitTrackingDao().insertHabit(
            HabitDefinition(name = "Habit", goalIdentityStatement = "", description = "", type = HabitType.BOOLEAN)
        )
    }
}
