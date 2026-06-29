package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.Checklist
import com.example.habitpower.data.model.ChecklistItem
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskAndChecklistTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    private suspend fun userId(): Long =
        db.database.userDao().insertUser(UserProfile(name = "User"))

    @Test
    fun createList_addTasks_allAppear() = runBlocking {
        val userId = userId()
        val listId = repo.insertTaskList(TaskList(userId = userId, name = "Shopping"))
        repo.insertTask(Task(taskListId = listId, name = "Milk"))
        repo.insertTask(Task(taskListId = listId, name = "Eggs"))
        repo.insertTask(Task(taskListId = listId, name = "Bread"))

        val tasks = repo.getTasksForList(listId).first()
        assertEquals(3, tasks.size)
        assertTrue(tasks.any { it.name == "Milk" })
        assertTrue(tasks.any { it.name == "Eggs" })
        assertTrue(tasks.any { it.name == "Bread" })
    }

    @Test
    fun toggleTask_completionStateChanges() = runBlocking {
        val userId = userId()
        val listId = repo.insertTaskList(TaskList(userId = userId, name = "Work"))
        val taskId = repo.insertTask(Task(taskListId = listId, name = "Send email"))

        val task = db.database.taskDao().getTaskById(taskId)!!
        assertFalse(task.isDone)

        repo.toggleTask(task)

        val updated = db.database.taskDao().getTaskById(taskId)!!
        assertTrue(updated.isDone)
    }

    @Test
    fun deleteTask_goneFromList() = runBlocking {
        val userId = userId()
        val listId = repo.insertTaskList(TaskList(userId = userId, name = "Personal"))
        val taskId = repo.insertTask(Task(taskListId = listId, name = "Call Mom"))

        val task = db.database.taskDao().getTaskById(taskId)!!
        repo.deleteTask(task)

        val tasks = repo.getTasksForList(listId).first()
        assertTrue(tasks.none { it.id == taskId })
    }

    @Test
    fun deleteList_tasksCascadeDeleted() = runBlocking {
        val userId = userId()
        val listId = repo.insertTaskList(TaskList(userId = userId, name = "Temp"))
        repo.insertTask(Task(taskListId = listId, name = "Task1"))
        repo.insertTask(Task(taskListId = listId, name = "Task2"))

        val list = repo.getTaskLists(userId).first().first()
        repo.deleteTaskList(list)

        val tasks = repo.getTasksForList(listId).first()
        assertTrue("Tasks must be deleted when list is deleted", tasks.isEmpty())
    }

    @Test
    fun createChecklist_addItems_resetChecklist() = runBlocking {
        val userId = userId()
        val checklistId = repo.insertChecklist(Checklist(userId = userId, name = "Morning Routine"))
        val item1Id = repo.insertChecklistItem(ChecklistItem(checklistId = checklistId, name = "Brush teeth"))
        val item2Id = repo.insertChecklistItem(ChecklistItem(checklistId = checklistId, name = "Make bed"))

        // Toggle both items
        val i1 = db.database.taskDao().getChecklistItemById(item1Id)!!
        val i2 = db.database.taskDao().getChecklistItemById(item2Id)!!
        repo.toggleChecklistItem(i1)
        repo.toggleChecklistItem(i2)

        val beforeReset = repo.getItemsForChecklist(checklistId).first()
        assertTrue(beforeReset.all { it.isChecked })

        // Reset
        repo.resetChecklist(checklistId)

        val afterReset = repo.getItemsForChecklist(checklistId).first()
        assertTrue("All items must be unchecked after reset", afterReset.none { it.isChecked })
    }
}
