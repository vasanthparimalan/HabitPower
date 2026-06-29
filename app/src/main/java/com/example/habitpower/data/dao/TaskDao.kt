package com.example.habitpower.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habitpower.data.model.Checklist
import com.example.habitpower.data.model.ChecklistItem
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // ── Task Lists ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM task_lists WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTaskLists(userId: Long): Flow<List<TaskList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskList(list: TaskList): Long

    @Update
    suspend fun updateTaskList(list: TaskList)

    @Delete
    suspend fun deleteTaskList(list: TaskList)

    // ── Tasks ───────────────────────────────────────────────────────────────
    @Query("SELECT * FROM tasks WHERE taskListId = :listId ORDER BY isDone ASC, name ASC")
    fun getTasksForList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM checklist_items WHERE id = :itemId LIMIT 1")
    suspend fun getChecklistItemById(itemId: Long): ChecklistItem?

    @Query("DELETE FROM tasks WHERE taskListId = :listId")
    suspend fun deleteTasksForList(listId: Long)

    // ── Checklists ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM checklists WHERE userId = :userId ORDER BY name ASC")
    fun getChecklists(userId: Long): Flow<List<Checklist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist): Long

    @Update
    suspend fun updateChecklist(checklist: Checklist)

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    // ── Checklist Items ─────────────────────────────────────────────────────
    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY `order` ASC, name ASC")
    fun getItemsForChecklist(checklistId: Long): Flow<List<ChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItem): Long

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId")
    suspend fun deleteItemsForChecklist(checklistId: Long)

    @Query("UPDATE checklist_items SET isChecked = 0, lastCheckedAt = NULL WHERE checklistId = :checklistId")
    suspend fun resetChecklist(checklistId: Long)

    @Query("SELECT * FROM task_lists")
    suspend fun getAllTaskLists(): List<TaskList>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM checklists")
    suspend fun getAllChecklists(): List<Checklist>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItems(): List<ChecklistItem>
}
