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
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.data.model.UserStats
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

data class ImportResult(
    val users: Int = 0,
    val lifeAreas: Int = 0,
    val habits: Int = 0,
    val entries: Int = 0,
    val healthStats: Int = 0,
    val chants: Int = 0,
    val routines: Int = 0,
    val tasks: Int = 0,
    val quotes: Int = 0
)

sealed class ImportState {
    object Idle : ImportState()
    object Importing : ImportState()
    data class Success(val result: ImportResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(
    private val repository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository,
    private val appContext: Context
) : ViewModel() {

    var state by mutableStateOf<ImportState>(ImportState.Idle)
        private set

    fun reset() { state = ImportState.Idle }

    private val hpexManager = HpexBackupManager(repository, gamificationRepository)

    // ── .hpex full backup ─────────────────────────────────────────────────────

    fun importFromHpex(uri: Uri) {
        viewModelScope.launch {
            state = ImportState.Importing
            try {
                val json = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: throw Exception("Could not read the file.")
                val r = hpexManager.import(json)
                state = ImportState.Success(
                    ImportResult(
                        users = r.users,
                        lifeAreas = r.lifeAreas,
                        habits = r.habits,
                        entries = r.entries,
                        healthStats = r.healthStats,
                        chants = r.chants,
                        routines = r.routines,
                        tasks = r.tasks,
                        quotes = r.quotes
                    )
                )
            } catch (e: Exception) {
                state = ImportState.Error(e.message ?: "Import failed. Check the file format.")
            }
        }
    }

    // ── JSON full backup (legacy) ─────────────────────────────────────────────

    fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            state = ImportState.Importing
            try {
                val json = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: throw Exception("Could not read the file.")

                val root = JSONObject(json)
                val userIdMap = mutableMapOf<Long, Long>()
                val habitIdMap = mutableMapOf<Long, Long>()
                val habitTypeMap = mutableMapOf<Long, HabitType>()
                var userCount = 0; var habitCount = 0; var entryCount = 0; var healthCount = 0

                root.optJSONArray("users")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val u = arr.getJSONObject(i)
                        val oldId = u.getLong("id")
                        val name = u.optString("name").takeIf { it.isNotBlank() } ?: "User ${i + 1}"
                        val isActive = u.optBoolean("isActive", false)
                        val newId = repository.createUser(name)
                        if (isActive) repository.getUserById(newId)?.let { repository.updateUser(it.copy(isActive = true)) }
                        userIdMap[oldId] = newId
                        userCount++
                    }
                }

                root.optJSONArray("habits")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val h = arr.getJSONObject(i)
                        val oldId = h.getLong("id")
                        val habitType = runCatching { HabitType.valueOf(h.getString("type")) }.getOrNull() ?: continue
                        val recurrenceType = runCatching { HabitRecurrenceType.valueOf(h.optString("recurrenceType", "DAILY")) }.getOrDefault(HabitRecurrenceType.DAILY)
                        val operator = runCatching { TargetOperator.valueOf(h.optString("operator", "GREATER_THAN_OR_EQUAL")) }.getOrDefault(TargetOperator.GREATER_THAN_OR_EQUAL)
                        val goalStatement = h.optString("goalIdentityStatement").takeIf { it.isNotBlank() }
                            ?: h.optString("name").takeIf { it.isNotBlank() } ?: "Build this habit"
                        val newId = repository.createHabit(
                            name = h.getString("name"),
                            goalIdentityStatement = goalStatement,
                            description = h.optString("description", ""),
                            type = habitType,
                            unit = h.optString("unit").takeIf { it.isNotBlank() },
                            targetValue = if (h.has("targetValue") && !h.isNull("targetValue")) h.getDouble("targetValue") else null,
                            operator = operator,
                            recurrenceType = recurrenceType
                        )
                        habitIdMap[oldId] = newId
                        habitTypeMap[oldId] = habitType
                        habitCount++
                    }
                }

                val userHabitSets = mutableMapOf<Long, MutableSet<Long>>()
                root.optJSONArray("habitEntries")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val e = arr.getJSONObject(i)
                        val newUserId = userIdMap[e.getLong("userId")] ?: continue
                        val newHabitId = habitIdMap[e.getLong("habitId")] ?: continue
                        userHabitSets.getOrPut(newUserId) { mutableSetOf() }.add(newHabitId)
                    }
                }
                userHabitSets.forEach { (uid, hids) -> repository.replaceAssignmentsForUser(uid, hids.toList()) }

                val entriesToImport = mutableListOf<DailyHabitEntry>()
                root.optJSONArray("habitEntries")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val e = arr.getJSONObject(i)
                        val newUserId = userIdMap[e.getLong("userId")] ?: continue
                        val newHabitId = habitIdMap[e.getLong("habitId")] ?: continue
                        val habitType = habitTypeMap[e.getLong("habitId")] ?: continue
                        val date = runCatching { LocalDate.parse(e.getString("date")) }.getOrNull() ?: continue
                        val boolVal = if (e.has("booleanValue") && !e.isNull("booleanValue")) e.getBoolean("booleanValue") else null
                        val numVal = if (e.has("numericValue") && !e.isNull("numericValue")) e.getDouble("numericValue") else null
                        val txtVal = if (e.has("textValue") && !e.isNull("textValue")) e.getString("textValue") else null
                        entriesToImport.add(DailyHabitEntry(
                            userId = newUserId, habitId = newHabitId, date = date,
                            booleanValue = if (habitType == HabitType.BOOLEAN || habitType == HabitType.ROUTINE) boolVal else null,
                            numericValue = if (habitType == HabitType.BOOLEAN || habitType == HabitType.TEXT || habitType == HabitType.ROUTINE) null else numVal,
                            textValue = if (habitType == HabitType.TEXT) txtVal?.trim() else null
                        ))
                        entryCount++
                    }
                }
                repository.importBulkEntries(entriesToImport)

                root.optJSONArray("healthStats")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val date = runCatching { LocalDate.parse(s.getString("date")) }.getOrNull() ?: continue
                        repository.saveDailyStat(DailyHealthStat(
                            date = date,
                            sleepHours = s.optDouble("sleepHours", 0.0).toFloat(),
                            stepsCount = s.optInt("stepsCount", 0),
                            meditationCompleted = s.optBoolean("meditationCompleted", false)
                        ))
                        healthCount++
                    }
                }

                root.optJSONArray("userStats")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val newUserId = userIdMap[s.getLong("userId")] ?: continue
                        val lastCheckIn = s.optString("lastCheckInDate").takeIf { it.isNotBlank() }
                            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        gamificationRepository.upsertStats(UserStats(
                            userId = newUserId,
                            currentStreak = s.optInt("currentStreak", 0),
                            longestStreak = s.optInt("longestStreak", 0),
                            totalXp = s.optInt("totalXp", 0),
                            level = s.optInt("level", 1),
                            totalHabitsCompleted = s.optInt("totalHabitsCompleted", 0),
                            totalDaysPerfect = s.optInt("totalDaysPerfect", 0),
                            lastCheckInDate = lastCheckIn
                        ))
                    }
                }

                state = ImportState.Success(ImportResult(users = userCount, habits = habitCount, entries = entryCount, healthStats = healthCount))
            } catch (e: Exception) {
                state = ImportState.Error(e.message ?: "Import failed. Check the file format.")
            }
        }
    }

    // ── Habits history CSV ────────────────────────────────────────────────────

    fun importFromHabitsCsv(uri: Uri) {
        viewModelScope.launch {
            state = ImportState.Importing
            try {
                val lines = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readLines()
                    ?: throw Exception("Could not read the file.")
                if (lines.size < 2) throw Exception("File is empty or has no data rows.")

                val userCache = mutableMapOf<String, Long>()
                val habitCache = mutableMapOf<String, Pair<Long, HabitType>>()
                val userHabitSets = mutableMapOf<Long, MutableSet<Long>>()
                val entriesToImport = mutableListOf<DailyHabitEntry>()
                var userCount = 0; var habitCount = 0; var entryCount = 0

                data class Row(
                    val date: LocalDate,
                    val userName: String,
                    val habitName: String,
                    val type: HabitType,
                    val value: String,
                    val target: String
                )

                val parsed = mutableListOf<Row>()
                for (line in lines.drop(1)) {
                    if (line.isBlank()) continue
                    val cols = line.parseCsvLine()
                    if (cols.size < 6) continue
                    val date = runCatching { LocalDate.parse(cols[0].trim()) }.getOrNull() ?: continue
                    val userName = cols[1].trim()
                    if (userName.isBlank()) continue
                    val habitName = cols[2].trim()
                    if (habitName.isBlank()) continue
                    val habitType = runCatching { HabitType.valueOf(cols[3].trim()) }.getOrNull() ?: continue
                    val value = cols[5].trim()
                    val target = if (cols.size > 6) cols[6].trim() else ""
                    parsed.add(Row(date, userName, habitName, habitType, value, target))
                }

                for (row in parsed) {
                    if (!userCache.containsKey(row.userName)) {
                        val newId = repository.createUser(row.userName)
                        if (userCount == 0) repository.getUserById(newId)?.let { repository.updateUser(it.copy(isActive = true)) }
                        userCache[row.userName] = newId
                        userCount++
                    }
                    if (!habitCache.containsKey(row.habitName)) {
                        val target = row.target.toDoubleOrNull()
                        val newId = repository.createHabit(
                            name = row.habitName,
                            goalIdentityStatement = row.habitName,
                            description = "",
                            type = row.type,
                            unit = null,
                            targetValue = target
                        )
                        habitCache[row.habitName] = Pair(newId, row.type)
                        habitCount++
                    }
                }

                for (row in parsed) {
                    val userId = userCache[row.userName] ?: continue
                    val (habitId, habitType) = habitCache[row.habitName] ?: continue
                    userHabitSets.getOrPut(userId) { mutableSetOf() }.add(habitId)
                    val entry = buildEntry(userId, habitId, row.date, habitType, row.value) ?: continue
                    entriesToImport.add(entry)
                    entryCount++
                }

                userHabitSets.forEach { (uid, hids) -> repository.replaceAssignmentsForUser(uid, hids.toList()) }
                repository.importBulkEntries(entriesToImport)

                state = ImportState.Success(ImportResult(users = userCount, habits = habitCount, entries = entryCount))
            } catch (e: Exception) {
                state = ImportState.Error(e.message ?: "Import failed.")
            }
        }
    }

    // ── Routines CSV ──────────────────────────────────────────────────────────

    fun importFromRoutinesCsv(uri: Uri) {
        viewModelScope.launch {
            state = ImportState.Importing
            try {
                val lines = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readLines()
                    ?: throw Exception("Could not read the file.")
                if (lines.size < 2) throw Exception("File is empty or has no data rows.")

                data class ExerciseRow(
                    val routineName: String,
                    val exerciseName: String,
                    val sets: Int?,
                    val reps: Int?,
                    val durationSeconds: Int?,
                    val order: Int
                )

                val rows = mutableListOf<ExerciseRow>()
                for (line in lines.drop(1)) {
                    if (line.isBlank()) continue
                    val cols = line.parseCsvLine()
                    if (cols.size < 2) continue
                    val routineName = cols[0].trim()
                    val exerciseName = cols[1].trim()
                    if (routineName.isBlank() || exerciseName.isBlank()) continue
                    rows.add(ExerciseRow(
                        routineName = routineName,
                        exerciseName = exerciseName,
                        sets = if (cols.size > 2) cols[2].trim().toIntOrNull() else null,
                        reps = if (cols.size > 3) cols[3].trim().toIntOrNull() else null,
                        durationSeconds = if (cols.size > 4) cols[4].trim().toIntOrNull() else null,
                        order = if (cols.size > 5) cols[5].trim().toIntOrNull() ?: 0 else 0
                    ))
                }

                var routineCount = 0
                val grouped = rows.groupBy { it.routineName }
                for ((routineName, exercises) in grouped) {
                    val routineId = repository.insertRoutine(Routine(name = routineName, description = ""))
                    for (row in exercises.sortedBy { it.order }) {
                        if (row.exerciseName.isBlank()) continue
                        val exerciseId = repository.insertExercise(Exercise(
                            name = row.exerciseName,
                            description = "",
                            imageUri = null
                        ))
                        repository.addExerciseToRoutine(
                            routineId = routineId,
                            exerciseId = exerciseId,
                            order = row.order,
                            sets = row.sets,
                            reps = row.reps,
                            durationSeconds = row.durationSeconds
                        )
                    }
                    routineCount++
                }

                state = ImportState.Success(ImportResult(routines = routineCount))
            } catch (e: Exception) {
                state = ImportState.Error(e.message ?: "Import failed.")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildEntry(
        userId: Long, habitId: Long, date: LocalDate,
        habitType: HabitType, value: String
    ): DailyHabitEntry? {
        return when (habitType) {
            HabitType.BOOLEAN, HabitType.ROUTINE -> {
                if (value.lowercase() != "true") return null
                DailyHabitEntry(userId = userId, habitId = habitId, date = date, booleanValue = true)
            }
            HabitType.NUMBER, HabitType.COUNT, HabitType.DURATION,
            HabitType.TIMER, HabitType.POMODORO, HabitType.TIME -> {
                val num = value.toDoubleOrNull() ?: return null
                DailyHabitEntry(userId = userId, habitId = habitId, date = date, numericValue = num)
            }
            HabitType.TEXT -> {
                if (value.isBlank()) return null
                DailyHabitEntry(userId = userId, habitId = habitId, date = date, textValue = value)
            }
        }
    }

    private fun String.parseCsvLine(): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < length) {
            val c = this[i]
            when {
                c == '"' && inQuotes && i + 1 < length && this[i + 1] == '"' -> { current.append('"'); i += 2 }
                c == '"' -> { inQuotes = !inQuotes; i++ }
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear(); i++ }
                else -> { current.append(c); i++ }
            }
        }
        result.add(current.toString())
        return result
    }
}
