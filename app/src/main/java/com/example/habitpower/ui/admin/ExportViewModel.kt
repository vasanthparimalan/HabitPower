package com.example.habitpower.ui.admin

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class ExportFormat { CSV, JSON }

data class PendingExport(
    val content: String,
    val suggestedFileName: String,
    val mimeType: String
)

data class ExportUiState(
    val isPreparing: Boolean = false,
    val pendingExport: PendingExport? = null,
    val resultMessage: String? = null
)

class ExportViewModel(
    private val repository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository,
    private val appContext: Context
) : ViewModel() {

    var uiState by mutableStateOf(ExportUiState())
        private set

    fun prepareExport(format: ExportFormat) {
        viewModelScope.launch {
            uiState = uiState.copy(isPreparing = true, pendingExport = null, resultMessage = null)
            try {
                val content = when (format) {
                    ExportFormat.CSV -> buildCsv()
                    ExportFormat.JSON -> buildJson()
                }
                val date = LocalDate.now().toString()
                val (fileName, mimeType) = when (format) {
                    ExportFormat.CSV -> "habitpower_export_$date.csv" to "text/csv"
                    ExportFormat.JSON -> "habitpower_backup_$date.json" to "application/json"
                }
                uiState = uiState.copy(
                    isPreparing = false,
                    pendingExport = PendingExport(content, fileName, mimeType)
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isPreparing = false,
                    resultMessage = "Could not prepare export: ${e.message}"
                )
            }
        }
    }

    fun writeTo(uri: Uri) {
        val content = uiState.pendingExport?.content ?: return
        viewModelScope.launch {
            try {
                appContext.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
                uiState = uiState.copy(pendingExport = null, resultMessage = "File saved successfully.")
            } catch (e: Exception) {
                uiState = uiState.copy(pendingExport = null, resultMessage = "Save failed: ${e.message}")
            }
        }
    }

    fun clearResult() {
        uiState = uiState.copy(resultMessage = null)
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    private suspend fun buildCsv(): String {
        val users = repository.getAllUsers().first()
        val habits = repository.getAllHabits().first()
        val habitById = habits.associateBy { it.id }
        val userById = users.associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("date,user_name,habit_name,habit_type,life_area_id,boolean_value,numeric_value,text_value")

        users.forEach { user ->
            repository.getAllEntriesForUser(user.id).forEach { entry ->
                val habit = habitById[entry.habitId]
                sb.appendLine(
                    "${entry.date}," +
                    "${(userById[entry.userId]?.name ?: entry.userId).csvEscape()}," +
                    "${(habit?.name ?: entry.habitId).toString().csvEscape()}," +
                    "${habit?.type?.name ?: "UNKNOWN"}," +
                    "${habit?.lifeAreaId ?: ""}," +
                    "${entry.booleanValue ?: ""}," +
                    "${entry.numericValue ?: ""}," +
                    "${entry.textValue?.csvEscape() ?: ""}"
                )
            }
        }
        return sb.toString()
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private suspend fun buildJson(): String {
        val users = repository.getAllUsers().first()
        val habits = repository.getAllHabits().first()
        val healthStats = repository.getAllHealthStats().first()

        val root = JSONObject()
        root.put("exportedAt", LocalDate.now().toString())
        root.put("appVersion", "1.0")

        // Users
        val usersArray = JSONArray()
        users.forEach { user ->
            usersArray.put(JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("isActive", user.isActive)
            })
        }
        root.put("users", usersArray)

        // Habits
        val habitsArray = JSONArray()
        habits.forEach { habit ->
            habitsArray.put(JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("type", habit.type.name)
                put("description", habit.description)
                put("goalIdentityStatement", habit.goalIdentityStatement)
                put("recurrenceType", habit.recurrenceType.name)
                habit.targetValue?.let { put("targetValue", it) }
                habit.unit?.let { put("unit", it) }
                put("operator", habit.operator.name)
                habit.lifeAreaId?.let { put("lifeAreaId", it) }
                put("isActive", habit.isActive)
            })
        }
        root.put("habits", habitsArray)

        // Habit entries (all users)
        val entriesArray = JSONArray()
        users.forEach { user ->
            repository.getAllEntriesForUser(user.id).forEach { entry ->
                entriesArray.put(JSONObject().apply {
                    put("userId", entry.userId)
                    put("habitId", entry.habitId)
                    put("date", entry.date.toString())
                    entry.booleanValue?.let { put("booleanValue", it) }
                    entry.numericValue?.let { put("numericValue", it) }
                    entry.textValue?.let { put("textValue", it) }
                })
            }
        }
        root.put("habitEntries", entriesArray)

        // Health stats
        val healthArray = JSONArray()
        healthStats.forEach { stat ->
            healthArray.put(JSONObject().apply {
                put("date", stat.date.toString())
                put("sleepHours", stat.sleepHours)
                put("stepsCount", stat.stepsCount)
                put("meditationCompleted", stat.meditationCompleted)
            })
        }
        root.put("healthStats", healthArray)

        // User stats / gamification progress
        val statsArray = JSONArray()
        users.forEach { user ->
            val stats = gamificationRepository.getStats(user.id)
            statsArray.put(JSONObject().apply {
                put("userId", user.id)
                put("userName", user.name)
                put("currentStreak", stats.currentStreak)
                put("longestStreak", stats.longestStreak)
                put("totalXp", stats.totalXp)
                put("level", stats.level)
                put("totalHabitsCompleted", stats.totalHabitsCompleted)
                put("totalDaysPerfect", stats.totalDaysPerfect)
                stats.lastCheckInDate?.let { put("lastCheckInDate", it.toString()) }
            })
        }
        root.put("userStats", statsArray)

        return root.toString(2)
    }
}

private fun Any.csvEscape(): String {
    val s = this.toString()
    return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
        "\"${s.replace("\"", "\"\"")}\""
    } else s
}
