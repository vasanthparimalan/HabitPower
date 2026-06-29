package com.example.habitpower.ui.admin

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.HpexBackupManager
import com.example.habitpower.data.HpexSection
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class ExportFormat { CSV, ROUTINES_CSV, HEALTH_CSV, JSON }

data class PendingExport(
    val content: String,
    val suggestedFileName: String,
    val mimeType: String
)

data class ExportUiState(
    val isPreparing: Boolean = false,
    val pendingExport: PendingExport? = null,
    val resultMessage: String? = null,
    val selectedHpexSections: Set<HpexSection> = HpexSection.entries.toSet()
)

class ExportViewModel(
    private val repository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository,
    private val appContext: Context
) : ViewModel() {

    var uiState by mutableStateOf(ExportUiState())
        private set

    private val hpexManager = HpexBackupManager(repository, gamificationRepository)

    fun toggleHpexSection(section: HpexSection) {
        val current = uiState.selectedHpexSections
        uiState = uiState.copy(
            selectedHpexSections = if (section in current) current - section else current + section
        )
    }

    fun prepareHpexExport() {
        val sections = uiState.selectedHpexSections
        if (sections.isEmpty()) {
            uiState = uiState.copy(resultMessage = "Select at least one section to export.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isPreparing = true, pendingExport = null, resultMessage = null)
            try {
                val date = LocalDate.now().toString()
                val content = hpexManager.export(sections, "1.7")
                uiState = uiState.copy(
                    isPreparing = false,
                    pendingExport = PendingExport(content, "habitpower_backup_$date.hpex", "application/octet-stream")
                )
            } catch (e: Exception) {
                uiState = uiState.copy(isPreparing = false, resultMessage = "Could not prepare backup: ${e.message}")
            }
        }
    }

    fun prepareExport(format: ExportFormat) {
        viewModelScope.launch {
            uiState = uiState.copy(isPreparing = true, pendingExport = null, resultMessage = null)
            try {
                val date = LocalDate.now().toString()
                val (content, fileName, mimeType) = when (format) {
                    ExportFormat.CSV -> Triple(buildHabitCsv(), "habitpower_habits_$date.csv", "text/csv")
                    ExportFormat.ROUTINES_CSV -> Triple(buildRoutinesCsv(), "habitpower_routines_$date.csv", "text/csv")
                    ExportFormat.HEALTH_CSV -> Triple(buildHealthCsv(), "habitpower_health_$date.csv", "text/csv")
                    ExportFormat.JSON -> Triple(buildJson(), "habitpower_backup_$date.json", "application/json")
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

    private suspend fun buildHabitCsv(): String {
        val users = repository.getAllUsers().first()
        val habits = repository.getAllHabits().first()
        val lifeAreas = repository.getAllLifeAreas().first()
        val habitById = habits.associateBy { it.id }
        val userById = users.associateBy { it.id }
        val lifeAreaById = lifeAreas.associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("date,user,habit_name,type,life_area,value,target,completed")

        data class Row(val date: java.time.LocalDate, val line: String)
        val rows = mutableListOf<Row>()

        users.forEach { user ->
            val userName = (userById[user.id]?.name ?: user.id.toString()).csvEscape()
            repository.getAllEntriesForUser(user.id).forEach { entry ->
                val habit = habitById[entry.habitId]
                val lifeArea = habit?.lifeAreaId?.let { lifeAreaById[it]?.name } ?: ""
                val value = when {
                    entry.booleanValue != null -> entry.booleanValue.toString()
                    entry.numericValue != null -> entry.numericValue.toString()
                    entry.textValue != null -> entry.textValue.csvEscape()
                    else -> ""
                }
                val target = habit?.targetValue?.toString() ?: ""
                val completed = when {
                    entry.booleanValue == true -> "true"
                    entry.numericValue != null && habit?.targetValue != null ->
                        (entry.numericValue >= habit.targetValue).toString()
                    else -> ""
                }
                rows += Row(
                    date = entry.date,
                    line = "${entry.date},$userName,${(habit?.name ?: entry.habitId.toString()).csvEscape()},${habit?.type?.name ?: "UNKNOWN"},${lifeArea.csvEscape()},$value,$target,$completed"
                )
            }
        }
        rows.sortedByDescending { it.date }.forEach { sb.appendLine(it.line) }
        return sb.toString()
    }

    private suspend fun buildRoutinesCsv(): String {
        val routines = repository.getAllRoutines().first()
        val sb = StringBuilder()
        sb.appendLine("routine_name,exercise,sets,reps,duration_s,order")
        routines.forEach { routine ->
            val exercises = repository.getRoutineExercisesWithDetails(routine.id).first()
            exercises.forEach { item ->
                sb.appendLine(
                    "${routine.name.csvEscape()}," +
                    "${item.exercise.name.csvEscape()}," +
                    "${item.crossRef.sets ?: ""}," +
                    "${item.crossRef.reps ?: ""}," +
                    "${item.crossRef.durationSeconds ?: ""}," +
                    "${item.crossRef.order}"
                )
            }
        }
        return sb.toString()
    }

    private suspend fun buildHealthCsv(): String {
        val stats = repository.getAllHealthStats().first()
        val sb = StringBuilder()
        sb.appendLine("date,sleep_hours,steps")
        stats.sortedByDescending { it.date }.forEach { s ->
            sb.appendLine("${s.date},${s.sleepHours},${s.stepsCount}")
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

        val usersArray = JSONArray()
        users.forEach { user ->
            usersArray.put(JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("isActive", user.isActive)
            })
        }
        root.put("users", usersArray)

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
